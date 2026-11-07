package tn.esprit.pi5eme.service.portfolio;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.config.PortfolioProperties;
import tn.esprit.pi5eme.dto.PageResponse;
import tn.esprit.pi5eme.dto.marketdata.QuoteResponse;
import tn.esprit.pi5eme.dto.portfolio.CashOperationRequest;
import tn.esprit.pi5eme.dto.portfolio.PortfolioSummaryResponse;
import tn.esprit.pi5eme.dto.portfolio.TransactionResponse;
import tn.esprit.pi5eme.entity.portfolio.*;
import tn.esprit.pi5eme.event.PortfolioChangedEvent;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.exception.InsufficientFundsException;
import tn.esprit.pi5eme.repository.portfolio.*;
import tn.esprit.pi5eme.scheduler.MarketCalendar;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioServiceImpl implements IPortfolioService {

    private final PortfolioProvisioner provisioner;
    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final CashReservationRepository reservationRepository;
    private final AllocationTargetRepository targetRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final PortfolioValuator valuator;
    private final PnlCalculator calc;
    private final IMarketDataService marketDataService;
    private final PortfolioProperties props;
    private final org.springframework.context.ApplicationEventPublisher publisher;
    private final Clock clock;

    @Override
    @Transactional
    public PortfolioSummaryResponse getSummary(Long userId) {
        return buildSummary(provisioner.getOrCreate(userId));
    }

    @Override
    @Transactional
    public TransactionResponse deposit(Long userId, CashOperationRequest req) {
        Portfolio p = provisioner.getOrCreate(userId);
        BigDecimal amount = MoneyUtils.money(req.getAmount());
        if (amount.compareTo(props.getMaxCashOperation()) > 0) {
            throw new BadRequestException("Deposit exceeds the maximum of " + props.getMaxCashOperation() + " TND");
        }
        p.setCashBalance(p.getCashBalance().add(amount));
        p.setNetContributions(p.getNetContributions().add(amount));
        portfolioRepository.save(p);
        PortfolioTransaction t = transactionRepository.save(cashRow(p, TransactionType.DEPOSIT, amount,
                amount, req.getNote()));
        publisher.publishEvent(new PortfolioChangedEvent(userId, p.getId(), "DEPOSIT"));
        log.info("User {} deposited {} TND", userId, amount);
        return TransactionResponse.fromEntity(t);
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(Long userId, CashOperationRequest req) {
        Portfolio p = provisioner.getOrCreate(userId);
        BigDecimal amount = MoneyUtils.money(req.getAmount());
        if (amount.compareTo(p.getAvailableCash()) > 0) {
            throw new InsufficientFundsException("Insufficient funds: requested " + amount
                    + " TND, available " + p.getAvailableCash() + " TND");
        }
        p.setCashBalance(p.getCashBalance().subtract(amount));
        p.setNetContributions(p.getNetContributions().subtract(amount));
        portfolioRepository.save(p);
        PortfolioTransaction t = transactionRepository.save(cashRow(p, TransactionType.WITHDRAWAL, amount,
                amount.negate(), req.getNote()));
        publisher.publishEvent(new PortfolioChangedEvent(userId, p.getId(), "WITHDRAWAL"));
        log.info("User {} withdrew {} TND", userId, amount);
        return TransactionResponse.fromEntity(t);
    }

    @Override
    @Transactional
    public PortfolioSummaryResponse reset(Long userId) {
        Portfolio p = provisioner.getOrCreate(userId);
        if (!reservationRepository.findByPortfolioAndStatus(p, ReservationStatus.ACTIVE).isEmpty()) {
            throw new BadRequestException("Cancel your open orders before resetting");
        }
        Instant now = Instant.now(clock);
        for (Position pos : positionRepository.findByPortfolioAndStatusOrderByTickerAsc(p, PositionStatus.OPEN)) {
            pos.setStatus(PositionStatus.CLOSED);
            pos.setClosedAt(now);
            pos.setReservedQuantity(0);
            positionRepository.save(pos);
        }
        BigDecimal capital = MoneyUtils.money(p.getInitialCapital());
        p.setCashBalance(capital);
        p.setNetContributions(capital);
        p.setRealizedPnl(MoneyUtils.money(BigDecimal.ZERO));
        p.setTotalFees(MoneyUtils.money(BigDecimal.ZERO));
        p.setReservedCash(MoneyUtils.money(BigDecimal.ZERO));
        portfolioRepository.save(p);
        targetRepository.deleteByPortfolio(p);
        // A snapshot already taken today holds pre-reset values and would show on the performance chart.
        snapshotRepository.deleteByPortfolioAndDateGreaterThanEqual(p, now.atZone(MarketCalendar.ZONE).toLocalDate());
        transactionRepository.save(PortfolioTransaction.builder()
                .portfolio(p).type(TransactionType.RESET)
                .grossAmount(capital).netAmount(MoneyUtils.money(BigDecimal.ZERO))
                .cashBalanceAfter(capital).note("reset").executedAt(now).build());
        publisher.publishEvent(new PortfolioChangedEvent(userId, p.getId(), "RESET"));
        log.info("Portfolio of user {} reset", userId);
        return buildSummary(p);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getTransactions(Long userId, TransactionType type, String ticker,
                                                             LocalDate from, LocalDate to, int page, int size) {
        Portfolio p = portfolioRepository.findByUserId(userId).orElse(null);
        if (p == null) {
            return PageResponse.<TransactionResponse>builder().content(List.of()).page(0)
                    .size(Math.max(1, Math.min(100, size))).totalElements(0).totalPages(0).build();
        }
        int effectiveSize = Math.max(1, Math.min(100, size));
        PageRequest pageable = PageRequest.of(Math.max(0, page), effectiveSize,
                Sort.by(Sort.Order.desc("executedAt"), Sort.Order.desc("id")));
        Specification<PortfolioTransaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("portfolio"), p));
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (ticker != null && !ticker.isBlank()) {
                predicates.add(cb.equal(root.get("ticker"), ticker.trim().toUpperCase(Locale.ROOT)));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("executedAt"),
                        from.atStartOfDay(MarketCalendar.ZONE).toInstant()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("executedAt"),
                        to.plusDays(1).atStartOfDay(MarketCalendar.ZONE).toInstant()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<PortfolioTransaction> result = transactionRepository.findAll(spec, pageable);
        return PageResponse.of(result, TransactionResponse::fromEntity);
    }

    private PortfolioSummaryResponse buildSummary(Portfolio p) {
        List<Position> open = positionRepository.findByPortfolioAndStatusOrderByTickerAsc(p, PositionStatus.OPEN);
        PortfolioValuator.Valuation v = valuator.value(p, open);

        BigDecimal unrealized = BigDecimal.ZERO;
        BigDecimal dayPnl = BigDecimal.ZERO;
        Map<String, QuoteResponse> quotes = open.isEmpty() ? Map.of()
                : marketDataService.getQuotes(open.stream().map(Position::getTicker).toList());
        for (Position pos : open) {
            BigDecimal last = v.pricesUsed().get(pos.getTicker());
            unrealized = unrealized.add(calc.unrealizedPnl(pos.getQuantity(), pos.getAvgCost(), last));
            QuoteResponse q = quotes.get(pos.getTicker().toUpperCase(Locale.ROOT));
            if (q != null && q.getPreviousClose() != null && !v.missingPrices().contains(pos.getTicker())) {
                dayPnl = dayPnl.add(BigDecimal.valueOf(pos.getQuantity()).multiply(last.subtract(q.getPreviousClose())));
            }
        }
        dayPnl = MoneyUtils.money(dayPnl);
        BigDecimal base = v.marketValue().subtract(dayPnl);
        BigDecimal totalPnl = MoneyUtils.money(v.totalValue().subtract(p.getNetContributions()));

        return PortfolioSummaryResponse.builder()
                .portfolioId(p.getId()).name(p.getName()).currency(p.getBaseCurrency())
                .initialCapital(p.getInitialCapital()).netContributions(p.getNetContributions())
                .cashBalance(p.getCashBalance()).reservedCash(p.getReservedCash()).availableCash(p.getAvailableCash())
                .marketValue(v.marketValue()).totalValue(v.totalValue())
                .realizedPnl(p.getRealizedPnl()).unrealizedPnl(MoneyUtils.money(unrealized))
                .totalPnl(totalPnl).totalReturnPct(MoneyUtils.percentOf(totalPnl, p.getNetContributions()))
                .dayPnl(dayPnl).dayPnlPct(base.signum() == 0 ? MoneyUtils.percent(BigDecimal.ZERO)
                        : MoneyUtils.percentOf(dayPnl, base))
                .totalFees(p.getTotalFees()).openPositionsCount(open.size())
                .missingPrices(v.missingPrices()).createdAt(p.getCreatedAt())
                .build();
    }

    private PortfolioTransaction cashRow(Portfolio p, TransactionType type, BigDecimal gross, BigDecimal net,
                                         String note) {
        return PortfolioTransaction.builder()
                .portfolio(p).type(type).grossAmount(gross).netAmount(net)
                .cashBalanceAfter(p.getCashBalance()).note(note).executedAt(Instant.now(clock)).build();
    }
}
