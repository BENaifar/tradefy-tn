package tn.esprit.pi5eme.service.marketdata.indicator;

import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.indicator.*;

import java.util.List;
import java.util.Map;

@Component
public class SmaIndicator implements TechnicalIndicator {

    private static final IndicatorDefinition DEFINITION = new IndicatorDefinition(
            "SMA", "Simple Moving Average", "Mean of the last N closing prices.",
            IndicatorPlacement.OVERLAY,
            List.of(new IndicatorParam("period", "Period", ParamType.INT, 20, 2, 500)),
            List.of(new IndicatorOutput("value", "SMA", OutputStyle.LINE)),
            List.of());

    @Override
    public IndicatorDefinition definition() {
        return DEFINITION;
    }

    @Override
    public int warmup(Map<String, Double> params) {
        return params.get("period").intValue() - 1;
    }

    @Override
    public Map<String, double[]> compute(List<Candle> candles, Map<String, Double> params) {
        return Map.of("value", IndicatorMath.sma(IndicatorMath.closes(candles), params.get("period").intValue()));
    }
}
