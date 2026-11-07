package tn.esprit.pi5eme.service.marketdata.indicator;

import tn.esprit.pi5eme.dto.marketdata.Candle;

import java.util.Arrays;
import java.util.List;

/** Shared maths for indicators. Values are double; NaN means not available. */
final class IndicatorMath {

    private IndicatorMath() {}

    static double[] closes(List<Candle> candles) {
        double[] v = new double[candles.size()];
        for (int i = 0; i < v.length; i++) {
            v[i] = candles.get(i).close().doubleValue();
        }
        return v;
    }

    static double[] nans(int length) {
        double[] v = new double[length];
        Arrays.fill(v, Double.NaN);
        return v;
    }

    /** Simple moving average with a running sum. NaN for i &lt; n-1. */
    static double[] sma(double[] v, int n) {
        double[] out = nans(v.length);
        double sum = 0;
        for (int i = 0; i < v.length; i++) {
            sum += v[i];
            if (i >= n) {
                sum -= v[i - n];
            }
            if (i >= n - 1) {
                out[i] = sum / n;
            }
        }
        return out;
    }

    /**
     * Exponential moving average with k = 2/(n+1), seeded with the SMA of the first n defined values.
     * Leading NaNs are skipped, so it also works on a series such as MACD that starts late.
     */
    static double[] ema(double[] v, int n) {
        double[] out = nans(v.length);
        int first = 0;
        while (first < v.length && Double.isNaN(v[first])) {
            first++;
        }
        int seed = first + n - 1;
        if (seed >= v.length) {
            return out;
        }
        double sum = 0;
        for (int i = first; i <= seed; i++) {
            sum += v[i];
        }
        out[seed] = sum / n;
        double k = 2.0 / (n + 1);
        for (int i = seed + 1; i < v.length; i++) {
            out[i] = v[i] * k + out[i - 1] * (1 - k);
        }
        return out;
    }
}
