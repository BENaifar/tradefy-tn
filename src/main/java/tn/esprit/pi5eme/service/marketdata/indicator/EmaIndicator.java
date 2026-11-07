package tn.esprit.pi5eme.service.marketdata.indicator;

import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.indicator.*;

import java.util.List;
import java.util.Map;

@Component
public class EmaIndicator implements TechnicalIndicator {

    private static final IndicatorDefinition DEFINITION = new IndicatorDefinition(
            "EMA", "Exponential Moving Average", "Moving average that weights recent closes more.",
            IndicatorPlacement.OVERLAY,
            List.of(new IndicatorParam("period", "Period", ParamType.INT, 20, 2, 500)),
            List.of(new IndicatorOutput("value", "EMA", OutputStyle.LINE)),
            List.of());

    @Override
    public IndicatorDefinition definition() {
        return DEFINITION;
    }

    @Override
    public int warmup(Map<String, Double> params) {
        return 3 * params.get("period").intValue();
    }

    @Override
    public Map<String, double[]> compute(List<Candle> candles, Map<String, Double> params) {
        return Map.of("value", IndicatorMath.ema(IndicatorMath.closes(candles), params.get("period").intValue()));
    }
}
