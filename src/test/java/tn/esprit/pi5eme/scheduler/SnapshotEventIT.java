package tn.esprit.pi5eme.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;
import tn.esprit.pi5eme.event.SessionClosedEvent;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.repository.portfolio.*;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;
import tn.esprit.pi5eme.service.portfolio.PortfolioProvisioner;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Not transactional on purpose: the listener runs after commit / as fallback. */
@SpringBootTest
class SnapshotEventIT {

    @MockBean IMarketDataService marketData;
    @MockBean MarketDataProvider provider;
    @Autowired ApplicationEventPublisher publisher;
    @Autowired PortfolioProvisioner provisioner;
    @Autowired PortfolioRepository portfolios;
    @Autowired PortfolioSnapshotRepository snapshots;
    @Autowired PortfolioTransactionRepository transactions;

    @AfterEach
    void cleanUp() {
        snapshots.deleteAll();
        transactions.deleteAll();
        portfolios.deleteAll();
    }

    @Test
    void sessionClosedCreatesSnapshotForEachPortfolio() {
        Portfolio a = provisioner.getOrCreate(61L);
        Portfolio b = provisioner.getOrCreate(62L);
        LocalDate day = LocalDate.of(2026, 3, 4);

        publisher.publishEvent(new SessionClosedEvent(day));

        assertTrue(snapshots.findByPortfolioAndDate(a, day).isPresent());
        assertTrue(snapshots.findByPortfolioAndDate(b, day).isPresent());
    }

    /** Real path: closeSession() is @Transactional, so the listener runs AFTER_COMMIT of that transaction. */
    @Test
    void closeSessionPersistsSnapshots() {
        Portfolio a = provisioner.getOrCreate(63L);

        syncService.closeSession();

        LocalDate today = calendar.today();
        assertTrue(snapshots.findByPortfolioAndDate(a, today).isPresent(),
                "snapshot written in AFTER_COMMIT listener must be committed");
    }

    @Autowired tn.esprit.pi5eme.service.marketdata.IMarketDataSyncService syncService;
    @Autowired MarketCalendar calendar;
}
