package tn.esprit.pi5eme.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.marketdata.InstrumentResponse;
import tn.esprit.pi5eme.dto.portfolio.HoldingView;
import tn.esprit.pi5eme.dto.portfolio.SnapshotView;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;
import tn.esprit.pi5eme.entity.portfolio.Position;
import tn.esprit.pi5eme.entity.portfolio.PositionStatus;
import tn.esprit.pi5eme.repository.portfolio.PortfolioSnapshotRepository;
import tn.esprit.pi5eme.repository.portfolio.PositionRepository;
import tn.esprit.pi5eme.scheduler.MarketCalendar;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortfolioQueryServiceImpl implements IPortfolioQueryService {

    private static final String UNCLASSIFIED = "Non classé";

    private final PortfolioProvisioner provisioner;
    private final PositionRepository positionRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final PortfolioValuator valuator;
    private final IMarketDataService marketDataService;
    private final MarketCalendar calendar;

    @Override
    @Transactional
    public List<HoldingView> getHoldings(Long userId) {
        Portfolio p = provisioner.getOrCreate(userId);
        List<Position> open = positionRepository.findByPortfolioAndStatusOrderByTickerAsc(p, PositionStatus.OPEN);
        if (open.isEmpty()) {
            return List.of();
        }
        PortfolioValuator.Valuation v = valuator.value(p, open);
        Map<String, InstrumentResponse> instruments =
                marketDataService.getInstruments(open.stream().map(Position::getTicker).toList());
        return open.stream().map(pos -> {
            BigDecimal last = v.pricesUsed().get(pos.getTicker());
            BigDecimal marketValue = MoneyUtils.money(last.multiply(BigDecimal.valueOf(pos.getQuantity())));
            InstrumentResponse ins = instruments.get(pos.getTicker().toUpperCase(Locale.ROOT));
            String sector = ins == null || ins.getSector() == null ? UNCLASSIFIED : ins.getSector();
            return new HoldingView(pos.getTicker(), sector, pos.getQuantity(), pos.getAvgCost(), last,
                    marketValue, MoneyUtils.percentOf(marketValue, v.totalValue()));
        }).toList();
    }

    @Override
    @Transactional
    public List<SnapshotView> getSnapshots(Long userId, LocalDate from, LocalDate to) {
        Portfolio p = provisioner.getOrCreate(userId);
        LocalDate start = from != null ? from : LocalDate.of(2000, 1, 1);
        LocalDate end = to != null ? to : calendar.today();
        return snapshotRepository.findByPortfolioAndDateBetweenOrderByDateAsc(p, start, end).stream()
                .map(s -> new SnapshotView(s.getDate(), s.getTotalValue(), s.getDailyReturnPct(),
                        s.getCumulativeReturnPct()))
                .toList();
    }

    @Override
    @Transactional
    public BigDecimal getPortfolioValue(Long userId) {
        Portfolio p = provisioner.getOrCreate(userId);
        return valuator.value(p, positionRepository.findByPortfolioAndStatusOrderByTickerAsc(p, PositionStatus.OPEN))
                .totalValue();
    }
}
