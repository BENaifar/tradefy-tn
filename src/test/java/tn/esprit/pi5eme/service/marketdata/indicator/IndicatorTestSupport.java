package tn.esprit.pi5eme.service.marketdata.indicator;

import tn.esprit.pi5eme.dto.marketdata.Candle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class IndicatorTestSupport {

    private IndicatorTestSupport() {}

    /** Daily-spaced candles whose open, high, low and close all equal the given values. */
    static List<Candle> candles(double... closes) {
        List<Candle> list = new ArrayList<>();
        for (int i = 0; i < closes.length; i++) {
            BigDecimal c = BigDecimal.valueOf(closes[i]);
            list.add(new Candle(1_700_000_000L + i * 86_400L, c, c, c, c, 1));
        }
        return list;
    }

    static double[] range(int from, int to) {
        double[] v = new double[to - from + 1];
        for (int i = 0; i < v.length; i++) {
            v[i] = from + i;
        }
        return v;
    }
}
