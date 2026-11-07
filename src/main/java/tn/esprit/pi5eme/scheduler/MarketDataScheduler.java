package tn.esprit.pi5eme.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.config.MarketDataProperties;
import tn.esprit.pi5eme.config.SectorSeeder;
import tn.esprit.pi5eme.exception.MarketDataUnavailableException;
import tn.esprit.pi5eme.service.marketdata.IMarketDataSyncService;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class MarketDataScheduler {

    private final IMarketDataSyncService syncService;
    private final MarketCalendar calendar;
    private final MarketDataProperties props;
    private final SectorSeeder sectorSeeder;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        if (!props.isSyncOnStartup()) {
            return;
        }
        try {
            syncService.refresh();
        } catch (MarketDataUnavailableException e) {
            log.warn("Startup market sync failed: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected error during startup market sync", e);
        }
        try {
            sectorSeeder.seed();
        } catch (RuntimeException e) {
            log.error("Sector seeding failed", e);
        }
    }

    @Scheduled(fixedDelayString = "${app.marketdata.poll-interval:PT30S}", initialDelayString = "PT20S")
    public void poll() {
        if (!calendar.isOpenNow()) {
            return;
        }
        try {
            syncService.refresh();
        } catch (MarketDataUnavailableException e) {
            log.warn("Market poll failed: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected error during market poll", e);
        }
    }

    @Scheduled(cron = "0 15 14 * * MON-FRI", zone = "Africa/Tunis")
    public void endOfSession() {
        if (!calendar.isTradingDay(calendar.today())) {
            return;
        }
        try {
            syncService.refresh();
        } catch (MarketDataUnavailableException e) {
            log.warn("End-of-session sync failed, closing with last known quotes: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected error in end-of-session sync", e);
        }
        try {
            syncService.closeSession();
        } catch (RuntimeException e) {
            log.error("Unexpected error closing session", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Africa/Tunis")
    public void purge() {
        try {
            syncService.purgeOldTicks();
        } catch (RuntimeException e) {
            log.error("Tick purge failed", e);
        }
    }
}
