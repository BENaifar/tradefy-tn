package tn.esprit.pi5eme.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.marketdata.InstrumentResponse;
import tn.esprit.pi5eme.dto.portfolio.*;
import tn.esprit.pi5eme.entity.portfolio.*;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.exception.ResourceNotFoundException;
import tn.esprit.pi5eme.repository.portfolio.AllocationTargetRepository;
import tn.esprit.pi5eme.repository.portfolio.PositionRepository;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationServiceImpl implements IAllocationService {

    static final String CASH = "CASH";
    static final String UNCLASSIFIED = "Non classé";

    private final PortfolioProvisioner provisioner;
    private final PositionRepository positionRepository;
    private final AllocationTargetRepository targetRepository;
    private final PortfolioValuator valuator;
    private final IMarketDataService marketDataService;

    @Override
    @Transactional
    public AllocationResponse getAllocation(Long userId, AllocationGroupBy by) {
        Portfolio p = provisioner.getOrCreate(userId);
        List<Position> open = positionRepository.findByPortfolioAndStatusOrderByTickerAsc(p, PositionStatus.OPEN);
        PortfolioValuator.Valuation v = valuator.value(p, open);
        Map<String, InstrumentResponse> instruments = open.isEmpty() ? Map.of()
                : marketDataService.getInstruments(open.stream().map(Position::getTicker).toList());

        Map<String, BigDecimal> values = new LinkedHashMap<>();
        for (Position pos : open) {
            BigDecimal value = v.pricesUsed().get(pos.getTicker()).multiply(BigDecimal.valueOf(pos.getQuantity()));
            InstrumentResponse ins = instruments.get(pos.getTicker().toUpperCase(Locale.ROOT));
            values.merge(groupKey(by, pos, ins), value, BigDecimal::add);
        }
        Map<String, BigDecimal> targets = targetsFor(p, by);
        BigDecimal targetSum = targets.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        for (String key : targets.keySet()) {
            values.putIfAbsent(key, BigDecimal.ZERO);
        }
        values.put(CASH, p.getCashBalance());

        List<AllocationSlice> slices = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : values.entrySet()) {
            BigDecimal value = MoneyUtils.money(e.getValue());
            BigDecimal weight = MoneyUtils.percentOf(value, v.totalValue());
            BigDecimal target = CASH.equals(e.getKey())
                    ? (targets.isEmpty() ? null : MoneyUtils.percent(MoneyUtils.HUNDRED.subtract(targetSum)))
                    : targets.get(e.getKey());
            slices.add(AllocationSlice.builder().key(e.getKey()).label(e.getKey()).value(value)
                    .weightPct(weight).targetPct(target)
                    .driftPct(target == null ? null : MoneyUtils.percent(weight.subtract(target))).build());
        }
        slices.sort(Comparator.comparing(AllocationSlice::getValue).reversed());
        return AllocationResponse.builder().groupBy(by).totalValue(v.totalValue()).slices(slices).build();
    }

    @Override
    @Transactional
    public List<AllocationTargetResponse> getTargets(Long userId) {
        Portfolio p = provisioner.getOrCreate(userId);
        return targetRepository.findByPortfolio(p).stream().map(AllocationTargetResponse::fromEntity).toList();
    }

    @Override
    @Transactional
    public List<AllocationTargetResponse> setTargets(Long userId, List<AllocationTargetRequest> requests) {
        Portfolio p = provisioner.getOrCreate(userId);
        List<AllocationTarget> entities = validate(p, requests == null ? List.of() : requests);
        targetRepository.deleteByPortfolio(p);
        targetRepository.flush();
        List<AllocationTarget> saved = targetRepository.saveAll(entities);
        log.info("User {} set {} allocation targets", userId, saved.size());
        return saved.stream().map(AllocationTargetResponse::fromEntity).toList();
    }

    @Override
    @Transactional
    public List<RebalanceSuggestion> getRebalanceSuggestions(Long userId) {
        Portfolio p = provisioner.getOrCreate(userId);
        Map<String, BigDecimal> targets = targetsFor(p, AllocationGroupBy.INSTRUMENT);
        if (targets.isEmpty()) {
            return List.of();
        }
        List<Position> open = positionRepository.findByPortfolioAndStatusOrderByTickerAsc(p, PositionStatus.OPEN);
        PortfolioValuator.Valuation v = valuator.value(p, open);
        Map<String, Position> held = new HashMap<>();
        open.forEach(pos -> held.put(pos.getTicker(), pos));

        Set<String> tickers = new TreeSet<>(targets.keySet());
        tickers.addAll(held.keySet());
        Map<String, BigDecimal> prices = new HashMap<>(marketDataService.getLastPrices(tickers));
        v.pricesUsed().forEach(prices::putIfAbsent);

        List<RebalanceSuggestion> result = new ArrayList<>();
        for (String ticker : tickers) {
            BigDecimal last = prices.get(ticker);
            if (last == null || last.signum() <= 0) {
                continue;
            }
            BigDecimal pct = targets.getOrDefault(ticker, BigDecimal.ZERO);
            Position pos = held.get(ticker);
            long qty = pos == null ? 0 : pos.getQuantity();
            BigDecimal targetValue = v.totalValue().multiply(pct).divide(MoneyUtils.HUNDRED, 10, MoneyUtils.RM);
            BigDecimal currentValue = last.multiply(BigDecimal.valueOf(qty));
            BigDecimal diff = targetValue.subtract(currentValue);
            long shares = diff.abs().divide(last, 0, RoundingMode.DOWN).longValue();
            if (diff.signum() < 0) {
                shares = Math.min(shares, qty);
            }
            if (shares == 0) {
                continue;
            }
            result.add(RebalanceSuggestion.builder()
                    .ticker(ticker).side(diff.signum() > 0 ? OrderSide.BUY : OrderSide.SELL)
                    .quantity(shares).estimatedPrice(MoneyUtils.money(last))
                    .estimatedAmount(MoneyUtils.money(last.multiply(BigDecimal.valueOf(shares))))
                    .currentWeightPct(MoneyUtils.percentOf(currentValue, v.totalValue()))
                    .targetWeightPct(MoneyUtils.percent(pct)).build());
        }
        return result;
    }

    private List<AllocationTarget> validate(Portfolio p, List<AllocationTargetRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        TargetType type = requests.get(0).getType();
        if (type == null) {
            throw new BadRequestException("Target type is required");
        }
        List<String> sectors = type == TargetType.SECTOR ? marketDataService.getSectors() : List.of();
        Set<String> keys = new HashSet<>();
        BigDecimal sum = BigDecimal.ZERO;
        List<AllocationTarget> result = new ArrayList<>();
        for (AllocationTargetRequest r : requests) {
            if (r.getType() != type) {
                throw new BadRequestException("All targets must have the same type");
            }
            if (r.getKey() == null || r.getKey().isBlank()) {
                throw new BadRequestException("Target key is required");
            }
            if (r.getPercent() == null || r.getPercent().signum() <= 0
                    || r.getPercent().compareTo(MoneyUtils.HUNDRED) > 0) {
                throw new BadRequestException("Target percent must be greater than 0 and at most 100");
            }
            String key = type == TargetType.INSTRUMENT ? r.getKey().trim().toUpperCase(Locale.ROOT) : r.getKey().trim();
            if (!keys.add(key)) {
                throw new BadRequestException("Duplicate target key: " + key);
            }
            if (type == TargetType.INSTRUMENT) {
                try {
                    marketDataService.getInstrument(key);
                } catch (ResourceNotFoundException e) {
                    throw new BadRequestException("Unknown ticker: " + key);
                }
            } else if (!UNCLASSIFIED.equals(key) && !sectors.contains(key)) {
                throw new BadRequestException("Unknown sector: " + key);
            }
            sum = sum.add(r.getPercent());
            result.add(AllocationTarget.builder().portfolio(p).type(type).targetKey(key)
                    .targetPercent(MoneyUtils.percent(r.getPercent())).build());
        }
        if (sum.compareTo(MoneyUtils.HUNDRED) > 0) {
            throw new BadRequestException("Targets sum to " + sum + "%, which exceeds 100%");
        }
        return result;
    }

    private Map<String, BigDecimal> targetsFor(Portfolio p, AllocationGroupBy by) {
        TargetType type = switch (by) {
            case INSTRUMENT -> TargetType.INSTRUMENT;
            case SECTOR -> TargetType.SECTOR;
            case ASSET_CLASS -> null;
        };
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        if (type == null) {
            return map;
        }
        for (AllocationTarget t : targetRepository.findByPortfolio(p)) {
            if (t.getType() == type) {
                map.put(t.getTargetKey(), t.getTargetPercent());
            }
        }
        return map;
    }

    private static String groupKey(AllocationGroupBy by, Position pos, InstrumentResponse ins) {
        return switch (by) {
            case INSTRUMENT -> pos.getTicker();
            case SECTOR -> ins == null || ins.getSector() == null ? UNCLASSIFIED : ins.getSector();
            case ASSET_CLASS -> ins == null || ins.getAssetClass() == null ? "EQUITY" : ins.getAssetClass().name();
        };
    }
}
