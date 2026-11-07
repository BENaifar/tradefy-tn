package tn.esprit.pi5eme.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tn.esprit.pi5eme.entity.portfolio.*;
import tn.esprit.pi5eme.repository.portfolio.PortfolioRepository;
import tn.esprit.pi5eme.repository.portfolio.PortfolioSnapshotRepository;
import tn.esprit.pi5eme.repository.portfolio.PortfolioTransactionRepository;
import tn.esprit.pi5eme.repository.portfolio.PositionRepository;
import tn.esprit.pi5eme.scheduler.MarketCalendar;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotServiceImpl implements ISnapshotService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final PortfolioValuator valuator;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public PortfolioSnapshot buildSnapshot(Portfolio p, LocalDate date) {
        List<Position> open = positionRepository.findByPortfolioAndStatusOrderByTickerAsc(p, PositionStatus.OPEN);
        PortfolioValuator.Valuation v = valuator.value(p, open);

        LocalDate resetDate = transactionRepository
                .findTopByPortfolioAndTypeOrderByExecutedAtDesc(p, TransactionType.RESET)
                .map(t -> t.getExecutedAt().atZone(MarketCalendar.ZONE).toLocalDate()).orElse(null);
        PortfolioSnapshot prev = snapshotRepository
                .findTopByPortfolioAndDateLessThanOrderByDateDesc(p, date)
                .filter(s -> resetDate == null || !s.getDate().isBefore(resetDate))
                .orElse(null);

        BigDecimal netFlow = MoneyUtils.money(BigDecimal.ZERO);
        BigDecimal daily = MoneyUtils.percent(BigDecimal.ZERO);
        if (prev != null) {
            Instant from = prev.getDate().plusDays(1).atStartOfDay(MarketCalendar.ZONE).toInstant();
            Instant to = date.plusDays(1).atStartOfDay(MarketCalendar.ZONE).toInstant();
            netFlow = MoneyUtils.money(transactionRepository.sumNetAmount(p,
                    List.of(TransactionType.DEPOSIT, TransactionType.WITHDRAWAL), from, to));
            if (prev.getTotalValue() != null && prev.getTotalValue().signum() > 0) {
                BigDecimal change = v.totalValue().subtract(prev.getTotalValue()).subtract(netFlow);
                daily = MoneyUtils.percentOf(change, prev.getTotalValue());
            }
        }
        BigDecimal cumulative = MoneyUtils.percentOf(v.totalValue().subtract(p.getNetContributions()),
                p.getNetContributions());
        return PortfolioSnapshot.builder()
                .portfolio(p).date(date).cash(v.cash()).marketValue(v.marketValue()).totalValue(v.totalValue())
                .netContributions(p.getNetContributions()).netFlow(netFlow)
                .dailyReturnPct(daily).cumulativeReturnPct(cumulative).build();
    }

    @Override
    @Transactional
    public PortfolioSnapshot takeSnapshot(Portfolio p, LocalDate date) {
        PortfolioSnapshot built = buildSnapshot(p, date);
        PortfolioSnapshot target = snapshotRepository.findByPortfolioAndDate(p, date).orElse(built);
        if (target != built) {
            target.setCash(built.getCash());
            target.setMarketValue(built.getMarketValue());
            target.setTotalValue(built.getTotalValue());
            target.setNetContributions(built.getNetContributions());
            target.setNetFlow(built.getNetFlow());
            target.setDailyReturnPct(built.getDailyReturnPct());
            target.setCumulativeReturnPct(built.getCumulativeReturnPct());
        }
        return snapshotRepository.save(target);
    }

    @Override
    public int takeAllSnapshots(LocalDate date) {
        // REQUIRES_NEW: this runs from an AFTER_COMMIT listener of closeSession(); with REQUIRED it would
        // join the already-committed transaction and the snapshots would never be written.
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        int written = 0;
        for (Portfolio portfolio : portfolioRepository.findAll()) {
            Long id = portfolio.getId();
            try {
                template.executeWithoutResult(status ->
                        takeSnapshot(portfolioRepository.findById(id).orElseThrow(), date));
                written++;
            } catch (RuntimeException e) {
                log.error("Snapshot failed for portfolio {}", id, e);
            }
        }
        log.info("Snapshots taken for {}: {} portfolios", date, written);
        return written;
    }
}
