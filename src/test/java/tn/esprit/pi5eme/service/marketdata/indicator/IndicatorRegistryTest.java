package tn.esprit.pi5eme.service.marketdata.indicator;

import org.junit.jupiter.api.Test;
import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.indicator.IndicatorDefinition;
import tn.esprit.pi5eme.dto.marketdata.indicator.IndicatorPlacement;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndicatorRegistryTest {

    private static TechnicalIndicator dummy(String code) {
        return new TechnicalIndicator() {
            @Override
            public IndicatorDefinition definition() {
                return new IndicatorDefinition(code, code, "", IndicatorPlacement.OVERLAY, List.of(), List.of(), List.of());
            }

            @Override
            public int warmup(Map<String, Double> params) {
                return 0;
            }

            @Override
            public Map<String, double[]> compute(List<Candle> candles, Map<String, Double> params) {
                return Map.of();
            }
        };
    }

    @Test
    void duplicateCodeFailsFast() {
        assertThrows(IllegalStateException.class,
                () -> new IndicatorRegistry(List.of(dummy("X"), dummy("x"))));
    }

    @Test
    void findIsCaseInsensitive() {
        IndicatorRegistry registry = new IndicatorRegistry(List.of(new SmaIndicator()));
        assertTrue(registry.find("sma").isPresent());
        assertTrue(registry.find("Sma").isPresent());
        assertTrue(registry.find("nope").isEmpty());
    }

    @Test
    void catalogIsSortedByCode() {
        IndicatorRegistry registry = new IndicatorRegistry(List.of(dummy("B"), dummy("A"), dummy("C")));
        assertEquals(List.of("A", "B", "C"), registry.catalog().stream().map(IndicatorDefinition::code).toList());
    }
}
