package tn.esprit.pi5eme.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import tn.esprit.pi5eme.event.SessionClosedEvent;
import tn.esprit.pi5eme.service.portfolio.ISnapshotService;

/** Takes a snapshot of every portfolio when the market session closes. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioSnapshotScheduler {

    private final ISnapshotService snapshotService;

    @TransactionalEventListener(fallbackExecution = true)
    public void onSessionClosed(SessionClosedEvent event) {
        try {
            snapshotService.takeAllSnapshots(event.sessionDate());
        } catch (RuntimeException e) {
            log.error("Snapshot run failed for {}", event.sessionDate(), e);
        }
    }
}
