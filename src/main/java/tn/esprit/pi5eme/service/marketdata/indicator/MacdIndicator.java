package tn.esprit.pi5eme.service.marketdata.indicator;

import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.indicator.*;
import tn.esprit.pi5eme.exception.BadRequestException;

import java.util.List;
import java.util.Map;

@Component
public class MacdIndicator implements TechnicalIndicator {

    private static final IndicatorDefinition DEFINITION = new IndicatorDefinition(
            "MACD", "MACD", "Difference of two EMAs, its signal line and the histogram between them.",
            IndicatorPlacement.PANE,
            List.of(new IndicatorParam("fast", "Fast", ParamType.INT, 12, 2, 100),
                    new IndicatorParam("slow", "Slow", ParamType.INT, 26, 3, 200),
                    new IndicatorParam("signal", "Signal", ParamType.INT, 9, 2, 100)),
            List.of(new IndicatorOutput("macd", "MACD", OutputStyle.LINE),
                    new IndicatorOutput("signal", "Signal", OutputStyle.LINE),
                    new IndicatorOutput("histogram", "Histogram", OutputStyle.HISTOGRAM)),
            List.of(0.0));

    @Override
    public IndicatorDefinition definition() {
        return DEFINITION;
    }

    @Override
    public void validate(Map<String, Double> params) {
        if (params.get("fast") >= params.get("slow")) {
            throw new BadRequestException("MACD fast period must be smaller than the slow period");
        }
    }

    @Override
    public int warmup(Map<String, Double> params) {
        return 3 * params.get("slow").intValue() + params.get("signal").intValue();
    }

    @Override
    public Map<String, double[]> compute(List<Candle> candles, Map<String, Double> params) {
        double[] close = IndicatorMath.closes(candles);
        double[] fast = IndicatorMath.ema(close, params.get("fast").intValue());
        double[] slow = IndicatorMath.ema(close, params.get("slow").intValue());
        double[] macd = IndicatorMath.nans(close.length);
        for (int i = 0; i < close.length; i++) {
            if (!Double.isNaN(fast[i]) && !Double.isNaN(slow[i])) {
                macd[i] = fast[i] - slow[i];
            }
        }
        double[] signal = IndicatorMath.ema(macd, params.get("signal").intValue());
        double[] histogram = IndicatorMath.nans(close.length);
        for (int i = 0; i < close.length; i++) {
            if (!Double.isNaN(macd[i]) && !Double.isNaN(signal[i])) {
                histogram[i] = macd[i] - signal[i];
            }
        }
        return Map.of("macd", macd, "signal", signal, "histogram", histogram);
    }
}
