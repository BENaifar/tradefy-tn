package tn.esprit.pi5eme.service.marketdata.indicator;

import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.indicator.IndicatorDefinition;

import java.util.List;
import java.util.Map;

/**
 * A technical indicator. Adding one means adding one {@code @Component} class implementing this interface;
 * the registry, the catalog endpoint and the frontend picker pick it up automatically.
 * Indicators use {@code double} internally: they are statistics, not money.
 */
public interface TechnicalIndicator {

    /** Static description: code, parameters with bounds, outputs, placement and reference lines. */
    IndicatorDefinition definition();

    /** Number of candles needed before the first valid output, for the given resolved parameters. */
    int warmup(Map<String, Double> params);

    /**
     * Pure computation on candles sorted ascending. Returns, for every output key, one value per candle;
     * {@link Double#NaN} marks values that are not available yet.
     */
    Map<String, double[]> compute(List<Candle> candles, Map<String, Double> params);

    /**
     * Cross-parameter validation, called after each parameter passed its own range check.
     * Throw {@link tn.esprit.pi5eme.exception.BadRequestException} when the combination is invalid.
     */
    default void validate(Map<String, Double> params) {
    }
}
