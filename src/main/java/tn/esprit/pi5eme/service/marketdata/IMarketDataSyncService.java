package tn.esprit.pi5eme.service.marketdata;

import java.time.Instant;

public interface IMarketDataSyncService {

    record SyncResult(int received, int newInstruments, int updated, String provider, Instant at) {}

    /** Fetches a snapshot from the provider and upserts instruments, quotes and ticks. */
    SyncResult refresh();

    /** Writes (upserts) the daily PriceBar for the current session from the current quotes. */
    void closeSession();

    /** Deletes intraday ticks older than the retention period. Returns the number deleted. */
    int purgeOldTicks();

    /** In-memory time of the last successful sync, or null before the first one. */
    Instant getLastSyncAt();
}
