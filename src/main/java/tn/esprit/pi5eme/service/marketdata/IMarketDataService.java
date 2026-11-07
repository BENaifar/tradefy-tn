package tn.esprit.pi5eme.service.marketdata;

import tn.esprit.pi5eme.dto.marketdata.InstrumentResponse;
import tn.esprit.pi5eme.dto.marketdata.QuoteResponse;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * Contract of the market data module for the portfolio, trading and risk modules.
 * All tickers are case-insensitive; map keys are always UPPERCASE.
 */
public interface IMarketDataService {

    /**
     * Latest quote of one instrument.
     *
     * @throws tn.esprit.pi5eme.exception.ResourceNotFoundException     if the ticker is unknown
     * @throws tn.esprit.pi5eme.exception.MarketDataUnavailableException if the instrument has no quote yet
     */
    QuoteResponse getQuote(String ticker);

    /**
     * Last traded price of one instrument.
     *
     * @throws tn.esprit.pi5eme.exception.ResourceNotFoundException     if the ticker is unknown
     * @throws tn.esprit.pi5eme.exception.MarketDataUnavailableException if the instrument has no quote yet
     */
    BigDecimal getLastPrice(String ticker);

    /** Last prices for many tickers at once. Tickers without a quote are simply absent from the map. */
    Map<String, BigDecimal> getLastPrices(Collection<String> tickers);

    /** Latest quotes for many tickers at once. Unknown or quote-less tickers are absent from the map. */
    Map<String, QuoteResponse> getQuotes(Collection<String> tickers);

    /**
     * Static data of one instrument (no quote attached).
     *
     * @throws tn.esprit.pi5eme.exception.ResourceNotFoundException if the ticker is unknown
     */
    InstrumentResponse getInstrument(String ticker);

    /** Static data for many tickers. Unknown tickers are absent from the map. */
    Map<String, InstrumentResponse> getInstruments(Collection<String> tickers);

    /** Distinct sector names currently assigned to instruments, sorted alphabetically. */
    java.util.List<String> getSectors();

    /** True while the BVMT session is open (or when market hours are ignored by configuration). */
    boolean isMarketOpen();
}
