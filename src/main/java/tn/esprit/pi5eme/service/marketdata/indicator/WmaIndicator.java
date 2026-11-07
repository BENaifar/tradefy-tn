package tn.esprit.pi5eme.service.marketdata.indicator;

import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.indicator.*;

import java.util.List;
import java.util.Map;

/** Added to prove extensibility: one class, no other change needed. */
@Component
public class WmaIndicator implements TechnicalIndicator {

    private static final IndicatorDefinition DEFINITION = new IndicatorDefinition(
            "WMA", "Weighted Moving Average", "Moving average with linearly increasing weights (newest close weighs most).",
            IndicatorPlacement.OVERLAY,
            List.of(new IndicatorParam("period", "Period", ParamType.INT, 20, 2, 500)),
            List.of(new IndicatorOutput("value", "WMA", OutputStyle.LINE)),
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
        int n = params.get("period").intValue();
        double[] close = IndicatorMath.closes(candles);
        double[] out = IndicatorMath.nans(close.length);
        double weightSum = n * (n + 1) / 2.0;
        for (int i = n - 1; i < close.length; i++) {
            double sum = 0;
            for (int w = 1; w <= n; w++) {
                sum += close[i - n + w] * w;
            }
            out[i] = sum / weightSum;
        }
        return Map.of("value", out);
    }
}
