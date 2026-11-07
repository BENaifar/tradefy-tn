package tn.esprit.pi5eme.service.marketdata.indicator;

import tn.esprit.pi5eme.dto.marketdata.CandleInterval;
import tn.esprit.pi5eme.dto.marketdata.indicator.IndicatorDefinition;
import tn.esprit.pi5eme.dto.marketdata.indicator.IndicatorResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Indicators computed on the backend so the chart, AI, risk and trading modules share one source of truth. */
public interface IIndicatorService {

    /** Every available indicator with its parameters; the frontend builds its picker from this. */
    List<IndicatorDefinition> getCatalog();

    /**
     * Computes one indicator over the candles of [from, to]. Warm-up candles are fetched before {@code from}
     * and trimmed, so the output starts exactly at {@code from}.
     *
     * @param rawParams parameter name to raw text value; missing names use the default
     * @throws tn.esprit.pi5eme.exception.ResourceNotFoundException if the indicator code or ticker is unknown
     * @throws tn.esprit.pi5eme.exception.BadRequestException        for unknown, non-numeric, non-integer
     *                                                              or out-of-range parameters
     */
    IndicatorResult compute(String ticker, String code, CandleInterval interval, LocalDate from, LocalDate to,
                            Map<String, String> rawParams);
}
