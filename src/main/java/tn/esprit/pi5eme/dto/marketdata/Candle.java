package tn.esprit.pi5eme.dto.marketdata;

import java.math.BigDecimal;

/**
 * One OHLCV candle.
 *
 * @param time epoch SECONDS (UTC) of the bucket start; daily candles use the session date at 00:00 Africa/Tunis
 */
public record Candle(long time, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, long volume) {}
