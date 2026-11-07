package tn.esprit.pi5eme.service.marketdata.indicator;

import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.indicator.*;

import java.util.List;
import java.util.Map;

/** Wilder's RSI. */
@Component
public class RsiIndicator implements TechnicalIndicator {

    private static final IndicatorDefinition DEFINITION = new IndicatorDefinition(
            "RSI", "Relative Strength Index", "Momentum oscillator between 0 and 100 (Wilder smoothing).",
            IndicatorPlacement.PANE,
            List.of(new IndicatorParam("period", "Period", ParamType.INT, 14, 2, 100)),
            List.of(new IndicatorOutput("value", "RSI", OutputStyle.LINE)),
            List.of(30.0, 70.0));

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
        int n = params.get("period").intValue();
        double[] close = IndicatorMath.closes(candles);
        double[] out = IndicatorMath.nans(close.length);
        if (close.length <= n) {
            return Map.of("value", out);
        }
        double avgGain = 0;
        double avgLoss = 0;
        for (int i = 1; i <= n; i++) {
            double delta = close[i] - close[i - 1];
            avgGain += Math.max(delta, 0);
            avgLoss += Math.max(-delta, 0);
        }
        avgGain /= n;
        avgLoss /= n;
        out[n] = rsi(avgGain, avgLoss);
        for (int i = n + 1; i < close.length; i++) {
            double delta = close[i] - close[i - 1];
            avgGain = (avgGain * (n - 1) + Math.max(delta, 0)) / n;
            avgLoss = (avgLoss * (n - 1) + Math.max(-delta, 0)) / n;
            out[i] = rsi(avgGain, avgLoss);
        }
        return Map.of("value", out);
    }

    private static double rsi(double avgGain, double avgLoss) {
        if (avgLoss == 0) {
            return avgGain == 0 ? 50 : 100;
        }
        return 100 - 100 / (1 + avgGain / avgLoss);
    }
}
