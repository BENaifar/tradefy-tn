package tn.esprit.pi5eme.service.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.portfolio.PerformanceResponse;
import tn.esprit.pi5eme.entity.portfolio.*;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.repository.portfolio.PortfolioSnapshotRepository;
import tn.esprit.pi5eme.repository.portfolio.PortfolioTransactionRepository;
import tn.esprit.pi5eme.scheduler.MarketCalendar;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PerformanceServiceImplTest {

    private static final long USER = 51L;

    @MockBean IMarketDataService marketData;
    @MockBean MarketDataProvider provider;
    @Autowired IPerformanceService service;
    @Autowired PortfolioProvisioner provisioner;
    @Autowired PortfolioSnapshotRepository snapshots;
    @Autowired PortfolioTransactionRepository transactions;
    @Autowired MarketCalendar calendar;

    private Portfolio p;
    private LocalDate today;

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    @BeforeEach
    void setUp() {
        p = provisioner.getOrCreate(USER);
        today = calendar.today();
        // portfolio worth exactly the last snapshot, so the live point adds a 0 % day
        p.setCashBalance(d("102020.000"));
        p.setNetContributions(d("100000.000"));
    }

    private void snapshot(LocalDate date, String total, String daily, String cumulative) {
        snapshots.save(PortfolioSnapshot.builder().portfolio(p).date(date).cash(d(total)).marketValue(d("0"))
                .totalValue(d(total)).netContributions(d("100000")).netFlow(d("0"))
                .dailyReturnPct(d(daily)).cumulativeReturnPct(d(cumulative)).build());
    }

    private Instant at(LocalDate day) {
        return day.atTime(12, 0).atZone(MarketCalendar.ZONE).toInstant();
    }

    @Test
    void dailyReturnsCompound() {
        snapshot(today.minusDays(3), "100000.000", "0.0000", "0.0000");
        snapshot(today.minusDays(2), "101000.000", "1.0000", "1.0000");
        snapshot(today.minusDays(1), "102020.000", "1.0099", "2.0200");
        PerformanceResponse r = service.getPerformance(USER, PerformanceRange.ALL);
        assertEquals(4, r.getPoints().size());
        assertEquals(d("2.0200"), r.getPeriodReturnPct());
        assertEquals(d("2.0200"), r.getTotalReturnPct());
    }

    @Test
    void resetDropsOlderPoints() {
        snapshot(today.minusDays(3), "100000.000", "0.0000", "0.0000");
        snapshot(today.minusDays(2), "101000.000", "1.0000", "1.0000");
        snapshot(today.minusDays(1), "102020.000", "1.0099", "2.0200");
        transactions.save(PortfolioTransaction.builder().portfolio(p).type(TransactionType.RESET)
                .grossAmount(d("100000")).netAmount(d("0")).cashBalanceAfter(d("100000"))
                .executedAt(at(today.minusDays(1))).build());
        PerformanceResponse r = service.getPerformance(USER, PerformanceRange.ALL);
        assertEquals(2, r.getPoints().size());
        assertEquals(today.minusDays(1), r.getPoints().get(0).getDate());
    }

    @Test
    void livePointAppendedWhenTodayMissing() {
        snapshot(today.minusDays(1), "102020.000", "0.0000", "2.0200");
        PerformanceResponse r = service.getPerformance(USER, PerformanceRange.W1);
        assertEquals(2, r.getPoints().size());
        assertEquals(today, r.getPoints().get(1).getDate());
        assertEquals(d("102020.000"), r.getEndValue());
    }

    @Test
    void winRate() {
        for (String pnl : new String[]{"10.000", "5.000", "1.000", "-4.000"}) {
            transactions.save(PortfolioTransaction.builder().portfolio(p).type(TransactionType.SELL).ticker("AAA")
                    .quantity(1L).price(d("10")).grossAmount(d("10")).netAmount(d("10")).realizedPnl(d(pnl))
                    .cashBalanceAfter(d("100000")).executedAt(Instant.now()).build());
        }
        PerformanceResponse r = service.getPerformance(USER, PerformanceRange.M1);
        assertEquals(3, r.getWinningTrades());
        assertEquals(1, r.getLosingTrades());
        assertEquals(d("75.0000"), r.getWinRatePct());
        assertNull(r.getBestPosition());
    }
}
