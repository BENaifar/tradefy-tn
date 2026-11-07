package tn.esprit.pi5eme.service.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.entity.portfolio.*;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.repository.portfolio.PortfolioSnapshotRepository;
import tn.esprit.pi5eme.repository.portfolio.PortfolioTransactionRepository;
import tn.esprit.pi5eme.scheduler.MarketCalendar;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class SnapshotServiceImplTest {

    private static final long USER = 41L;

    @MockBean IMarketDataService marketData;
    @MockBean MarketDataProvider provider;
    @Autowired ISnapshotService service;
    @Autowired PortfolioProvisioner provisioner;
    @Autowired PortfolioSnapshotRepository snapshots;
    @Autowired PortfolioTransactionRepository transactions;

    private Portfolio p;

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    @BeforeEach
    void setUp() {
        p = provisioner.getOrCreate(USER);
    }

    @Test
    void threeDayScenario() {
        LocalDate d1 = LocalDate.of(2026, 1, 5);
        LocalDate d2 = LocalDate.of(2026, 1, 6);
        LocalDate d3 = LocalDate.of(2026, 1, 7);

        PortfolioSnapshot s1 = service.takeSnapshot(p, d1);
        assertEquals(d("0.0000"), s1.getDailyReturnPct());
        assertEquals(d("0.0000"), s1.getCumulativeReturnPct());

        p.setCashBalance(d("101000.000"));
        PortfolioSnapshot s2 = service.takeSnapshot(p, d2);
        assertEquals(d("1.0000"), s2.getDailyReturnPct());

        p.setCashBalance(d("103020.000"));
        p.setNetContributions(d("101000.000"));
        transactions.save(PortfolioTransaction.builder().portfolio(p).type(TransactionType.DEPOSIT)
                .grossAmount(d("1000.000")).netAmount(d("1000.000")).cashBalanceAfter(d("103020.000"))
                .executedAt(d3.atTime(12, 0).atZone(MarketCalendar.ZONE).toInstant()).build());
        PortfolioSnapshot s3 = service.takeSnapshot(p, d3);
        assertEquals(d("1000.000"), s3.getNetFlow());
        assertEquals(d("1.0099"), s3.getDailyReturnPct());
        assertEquals(d("2.0000"), s3.getCumulativeReturnPct());
    }

    @Test
    void sameDateTwiceKeepsOneRow() {
        LocalDate day = LocalDate.of(2026, 1, 5);
        service.takeSnapshot(p, day);
        service.takeSnapshot(p, day);
        assertEquals(1, snapshots.findAll().stream().filter(s -> s.getPortfolio().getId().equals(p.getId())).count());
    }
}
