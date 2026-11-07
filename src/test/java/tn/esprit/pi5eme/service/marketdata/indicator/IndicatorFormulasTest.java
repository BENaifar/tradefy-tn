package tn.esprit.pi5eme.service.marketdata.indicator;

import org.junit.jupiter.api.Test;
import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.exception.BadRequestException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static tn.esprit.pi5eme.service.marketdata.indicator.IndicatorTestSupport.candles;
import static tn.esprit.pi5eme.service.marketdata.indicator.IndicatorTestSupport.range;

/** Hand-checked vectors from the plan. */
class IndicatorFormulasTest {

    private static final double EPS = 1e-9;

    private final SmaIndicator sma = new SmaIndicator();
    private final EmaIndicator ema = new EmaIndicator();
    private final BollingerIndicator boll = new BollingerIndicator();
    private final RsiIndicator rsi = new RsiIndicator();
    private final MacdIndicator macd = new MacdIndicator();

    private static void assertSeries(double[] expected, double[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            if (Double.isNaN(expected[i])) {
                assertTrue(Double.isNaN(actual[i]), "index " + i + " should be NaN but was " + actual[i]);
            } else {
                assertEquals(expected[i], actual[i], EPS, "index " + i);
            }
        }
    }

    private static final double N = Double.NaN;

    @Test
    void smaOfOneToTen() {
        double[] out = sma.compute(candles(range(1, 10)), Map.of("period", 3.0)).get("value");
        assertSeries(new double[]{N, N, 2, 3, 4, 5, 6, 7, 8, 9}, out);
    }

    @Test
    void emaOfLinearSeriesEqualsSma() {
        double[] out = ema.compute(candles(range(1, 10)), Map.of("period", 3.0)).get("value");
        assertSeries(new double[]{N, N, 2, 3, 4, 5, 6, 7, 8, 9}, out);
    }

    @Test
    void emaJumpGivesFifteen() {
        double[] out = ema.compute(candles(10, 10, 10, 20), Map.of("period", 3.0)).get("value");
        assertEquals(15.0, out[3], EPS);
    }

    @Test
    void bollingerConstantSeriesCollapses() {
        Map<String, double[]> out = boll.compute(candles(5, 5, 5, 5), Map.of("period", 3.0, "stdDev", 2.0));
        assertEquals(5.0, out.get("upper")[3], EPS);
        assertEquals(5.0, out.get("middle")[3], EPS);
        assertEquals(5.0, out.get("lower")[3], EPS);
    }

    @Test
    void bollingerUsesPopulationStdDev() {
        Map<String, double[]> out = boll.compute(candles(1, 2, 3), Map.of("period", 3.0, "stdDev", 2.0));
        assertEquals(2.0, out.get("middle")[2], EPS);
        assertEquals(3.6330, out.get("upper")[2], 1e-4);
        assertEquals(0.3670, out.get("lower")[2], 1e-4);
    }

    @Test
    void rsiOnRisingFallingAndFlatSeries() {
        Map<String, Double> p = Map.of("period", 14.0);
        double[] up = rsi.compute(candles(range(1, 30)), p).get("value");
        double[] down = rsi.compute(candles(range(1, 30)).stream().map(c -> c).toList().isEmpty() ? null
                : candles(reverse(range(1, 30))), p).get("value");
        double[] flat = rsi.compute(candles(constant(30, 7)), p).get("value");
        for (int i = 0; i < 14; i++) {
            assertTrue(Double.isNaN(up[i]));
        }
        for (int i = 14; i < 30; i++) {
            assertEquals(100.0, up[i], EPS);
            assertEquals(0.0, down[i], EPS);
            assertEquals(50.0, flat[i], EPS);
        }
    }

    @Test
    void rsiWilderSmoothingOnOneTwoOneTwo() {
        double[] out = rsi.compute(candles(1, 2, 1, 2), Map.of("period", 2.0)).get("value");
        assertEquals(50.0, out[2], EPS);
        assertEquals(75.0, out[3], EPS);
    }

    @Test
    void macdOfConstantSeriesIsZeroOnceDefined() {
        Map<String, double[]> out = macd.compute(candles(constant(30, 9)),
                Map.of("fast", 3.0, "slow", 6.0, "signal", 3.0));
        for (String key : List.of("macd", "signal", "histogram")) {
            double[] v = out.get(key);
            assertEquals(30, v.length);
            assertEquals(0.0, v[29], EPS, key);
        }
        assertTrue(Double.isNaN(out.get("macd")[4]));
        assertFalse(Double.isNaN(out.get("macd")[5]));
        assertTrue(Double.isNaN(out.get("signal")[6]));
        assertFalse(Double.isNaN(out.get("signal")[7]));
    }

    @Test
    void macdFastNotBelowSlowIsRejected() {
        assertThrows(BadRequestException.class,
                () -> macd.validate(Map.of("fast", 26.0, "slow", 12.0, "signal", 9.0)));
    }

    @Test
    void everyOutputHasOneValuePerCandle() {
        List<Candle> c = candles(range(1, 40));
        assertEquals(40, sma.compute(c, Map.of("period", 5.0)).get("value").length);
        assertEquals(40, ema.compute(c, Map.of("period", 5.0)).get("value").length);
        assertEquals(40, rsi.compute(c, Map.of("period", 5.0)).get("value").length);
        boll.compute(c, Map.of("period", 5.0, "stdDev", 2.0)).values().forEach(v -> assertEquals(40, v.length));
        macd.compute(c, Map.of("fast", 5.0, "slow", 10.0, "signal", 3.0)).values()
                .forEach(v -> assertEquals(40, v.length));
    }

    @Test
    void shortSeriesDoesNotCrash() {
        List<Candle> c = candles(1, 2);
        assertEquals(2, rsi.compute(c, Map.of("period", 14.0)).get("value").length);
        assertEquals(2, ema.compute(c, Map.of("period", 20.0)).get("value").length);
        assertEquals(2, macd.compute(c, Map.of("fast", 12.0, "slow", 26.0, "signal", 9.0)).get("macd").length);
    }

    private static double[] reverse(double[] v) {
        double[] r = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            r[i] = v[v.length - 1 - i];
        }
        return r;
    }

    private static double[] constant(int length, double value) {
        double[] v = new double[length];
        java.util.Arrays.fill(v, value);
        return v;
    }
}
