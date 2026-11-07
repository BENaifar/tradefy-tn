package tn.esprit.pi5eme.service.marketdata.indicator;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tn.esprit.pi5eme.service.marketdata.indicator.IndicatorTestSupport.candles;

class WmaIndicatorTest {

    @Test
    void weightsNewestCloseMost() {
        double[] out = new WmaIndicator().compute(candles(1, 2, 3, 4), Map.of("period", 3.0)).get("value");
        assertTrue(Double.isNaN(out[1]));
        assertEquals((1 + 4 + 9) / 6.0, out[2], 1e-9);
        assertEquals((2 + 6 + 12) / 6.0, out[3], 1e-9);
    }
}
