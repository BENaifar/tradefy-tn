package tn.esprit.pi5eme.dto.marketdata;

import java.time.Duration;

/** Candle timeframe. D1 and W1 are built from daily bars, the others from intraday ticks. */
public enum CandleInterval {
    M5(Duration.ofMinutes(5)), M15(Duration.ofMinutes(15)), M30(Duration.ofMinutes(30)),
    H1(Duration.ofHours(1)), H2(Duration.ofHours(2)), H4(Duration.ofHours(4)),
    D1(null), W1(null);

    private final Duration bucket;

    CandleInterval(Duration bucket) {
        this.bucket = bucket;
    }

    public boolean isIntraday() {
        return bucket != null;
    }

    /** Bucket length, or null for daily-based intervals. */
    public Duration bucket() {
        return bucket;
    }
}
