package tn.esprit.pi5eme.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.portfolio.EquityPoint;
import tn.esprit.pi5eme.dto.portfolio.PerformanceResponse;
import tn.esprit.pi5eme.dto.portfolio.PerformanceResponse.PositionPerformance;
import tn.esprit.pi5eme.entity.portfolio.*;
import tn.esprit.pi5eme.repository.portfolio.PortfolioSnapshotRepository;
import tn.esprit.pi5eme.repository.portfolio.PortfolioTransactionRepository;
import tn.esprit.pi5eme.repository.portfolio.PositionRepository;
import tn.esprit.pi5eme.scheduler.MarketCalendar;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl implements IPerformanceService {

    private final PortfolioProvisioner provisioner;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final PositionRepository positionRepository;
    private final ISnapshotService snapshotService;
    private final PortfolioValuator valuator;
    private final PnlCalculator calc;
    private final MarketCalendar calendar;

    @Override
    @Transactional
    public PerformanceResponse getPerformance(Long userId, PerformanceRange range) {
        Portfolio p = provisioner.getOrCreate(userId);
        LocalDate today = calendar.today();
        LocalDate from = fromDate(range, today);

        Instant resetAt = transactionRepository.findTopByPortfolioAndTypeOrderByExecutedAtDesc(p, TransactionType.RESET)
                .map(PortfolioTransaction::getExecutedAt).orElse(null);
        LocalDate resetDate = resetAt == null ? null : resetAt.atZone(MarketCalendar.ZONE).toLocalDate();

        List<PortfolioSnapshot> snapshots = snapshotRepository.findByPortfolioAndDateBetweenOrderByDateAsc(p, from, today)
                .stream().filter(s -> resetDate == null || !s.getDate().isBefore(resetDate)).toList();
        List<PortfolioSnapshot> series = new ArrayList<>(snapshots);
        if (series.stream().noneMatch(s -> s.getDate().equals(today))) {
            series.add(snapshotService.buildSnapshot(p, today));
        }

        List<EquityPoint> points = series.stream().map(s -> EquityPoint.builder()
                .date(s.getDate()).totalValue(s.getTotalValue()).cumulativeReturnPct(s.getCumulativeReturnPct())
                .build()).toList();

        BigDecimal growth = BigDecimal.ONE;
        for (int i = 1; i < series.size(); i++) {
            BigDecimal r = series.get(i).getDailyReturnPct();
            growth = growth.multiply(BigDecimal.ONE.add(r.divide(MoneyUtils.HUNDRED, 10, MoneyUtils.RM)));
        }
        BigDecimal periodReturn = series.size() < 2 ? MoneyUtils.percent(BigDecimal.ZERO)
                : MoneyUtils.percent(growth.subtract(BigDecimal.ONE).multiply(MoneyUtils.HUNDRED));

        List<Position> open = positionRepository.findByPortfolioAndStatusOrderByTickerAsc(p, PositionStatus.OPEN);
        PortfolioValuator.Valuation v = valuator.value(p, open);
        BigDecimal unrealized = BigDecimal.ZERO;
        PositionPerformance best = null;
        PositionPerformance worst = null;
        for (Position pos : open) {
            BigDecimal last = v.pricesUsed().get(pos.getTicker());
            unrealized = unrealized.add(calc.unrealizedPnl(pos.getQuantity(), pos.getAvgCost(), last));
            PositionPerformance pp = PositionPerformance.builder().ticker(pos.getTicker())
                    .unrealizedPnlPct(calc.unrealizedPnlPct(pos.getAvgCost(), last)).build();
            if (best == null || pp.getUnrealizedPnlPct().compareTo(best.getUnrealizedPnlPct()) > 0) {
                best = pp;
            }
            if (worst == null || pp.getUnrealizedPnlPct().compareTo(worst.getUnrealizedPnlPct()) < 0) {
                worst = pp;
            }
        }

        Instant since = resetAt != null ? resetAt : Instant.EPOCH;
        long wins = transactionRepository.countWinning(p, TransactionType.SELL, since);
        long losses = transactionRepository.countLosing(p, TransactionType.SELL, since);

        BigDecimal totalReturn = MoneyUtils.percentOf(v.totalValue().subtract(p.getNetContributions()),
                p.getNetContributions());
        return PerformanceResponse.builder()
                .range(range).from(from).to(today).points(points)
                .startValue(points.get(0).getTotalValue()).endValue(points.get(points.size() - 1).getTotalValue())
                .periodReturnPct(periodReturn).totalReturnPct(totalReturn)
                .realizedPnl(p.getRealizedPnl()).unrealizedPnl(MoneyUtils.money(unrealized))
                .bestPosition(best).worstPosition(worst)
                .winningTrades(wins).losingTrades(losses)
                .winRatePct(MoneyUtils.percentOf(BigDecimal.valueOf(wins), BigDecimal.valueOf(wins + losses)))
                .build();
    }

    private static LocalDate fromDate(PerformanceRange range, LocalDate today) {
        return switch (range) {
            case W1 -> today.minusDays(7);
            case M1 -> today.minusMonths(1);
            case M3 -> today.minusMonths(3);
            case M6 -> today.minusMonths(6);
            case YTD -> LocalDate.of(today.getYear(), 1, 1);
            case Y1 -> today.minusYears(1);
            case ALL -> LocalDate.of(2000, 1, 1);
        };
    }
}
