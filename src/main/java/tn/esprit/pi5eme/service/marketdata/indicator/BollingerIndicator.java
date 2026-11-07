package tn.esprit.pi5eme.service.marketdata.indicator;

import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.indicator.*;

import java.util.List;
import java.util.Map;

/** Bands use the population standard deviation (divide by n), as TradingView does. */
@Component
public class BollingerIndicator implements TechnicalIndicator {

    private static final IndicatorDefinition DEFINITION = new IndicatorDefinition(
            "BOLL", "Bollinger Bands", "SMA with bands at a number of standard deviations around it.",
            IndicatorPlacement.OVERLAY,
            List.of(new IndicatorParam("period", "Period", ParamType.INT, 20, 2, 500),
                    new IndicatorParam("stdDev", "Std deviations", ParamType.DECIMAL, 2.0, 0.5, 5)),
            List.of(new IndicatorOutput("upper", "Upper", OutputStyle.LINE),
                    new IndicatorOutput("middle", "Middle", OutputStyle.LINE),
                    new IndicatorOutput("lower", "Lower", OutputStyle.LINE)),
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
        double m = params.get("stdDev");
        double[] close = IndicatorMath.closes(candles);
        double[] middle = IndicatorMath.sma(close, n);
        double[] upper = IndicatorMath.nans(close.length);
        double[] lower = IndicatorMath.nans(close.length);
        for (int i = n - 1; i < close.length; i++) {
            double sumSq = 0;
            for (int j = i - n + 1; j <= i; j++) {
                double diff = close[j] - middle[i];
                sumSq += diff * diff;
            }
            double sigma = Math.sqrt(sumSq / n);
            upper[i] = middle[i] + m * sigma;
            lower[i] = middle[i] - m * sigma;
        }
        return Map.of("upper", upper, "middle", middle, "lower", lower);
    }
}
