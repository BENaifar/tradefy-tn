package tn.esprit.pi5eme.service.marketdata;

import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.CandleInterval;
import tn.esprit.pi5eme.dto.marketdata.CandleSeriesResponse;

import java.time.LocalDate;
import java.util.List;

/** Candlestick data for charts and for the indicator service. Dates are in Africa/Tunis. */
public interface ICandleService {

    /**
     * Candles with the live/truncated flags. Null from/to fall back to the default range of the interval
     * (to defaults to today).
     *
     * @throws tn.esprit.pi5eme.exception.ResourceNotFoundException if the ticker is unknown
     * @throws tn.esprit.pi5eme.exception.BadRequestException        if from is after to
     */
    CandleSeriesResponse getSeries(String ticker, CandleInterval interval, LocalDate from, LocalDate to);

    /** Same as getSeries, candles only. */
    List<Candle> getCandles(String ticker, CandleInterval interval, LocalDate from, LocalDate to);

    /**
     * Candles for [from, to] plus up to {@code extra} candles before {@code from}, for indicator warm-up.
     * Returns everything found; the caller trims.
     */
    List<Candle> getCandlesWithLookback(String ticker, CandleInterval interval, LocalDate from, LocalDate to, int extra);

    /** Default start date of an interval when the caller gives none. */
    LocalDate defaultFrom(CandleInterval interval, LocalDate to);
}
