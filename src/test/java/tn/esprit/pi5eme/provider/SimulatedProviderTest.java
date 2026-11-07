package tn.esprit.pi5eme.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tn.esprit.pi5eme.config.MarketDataProperties;
import tn.esprit.pi5eme.dto.marketdata.RawQuote;
import tn.esprit.pi5eme.repository.marketdata.QuoteRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimulatedProviderTest {

    @Test
    void hundredCallsStayWithinLimitsPositiveAndVolumeMonotonic() {
        QuoteRepository repo = mock(QuoteRepository.class);
        when(repo.findAllActiveWithInstrument()).thenReturn(List.of());
        MarketDataProperties props = new MarketDataProperties();
        props.setSimulatedSeed(42L);
        SimulatedProvider p = new SimulatedProvider(repo, props, new BvmtQuoteMapper(), new ObjectMapper(), Clock.systemUTC());

        Map<String, Long> lastVolume = new HashMap<>();
        for (int n = 0; n < 100; n++) {
            List<RawQuote> snapshot = p.fetchSnapshot();
            assertFalse(snapshot.isEmpty());
            for (RawQuote q : snapshot) {
                assertTrue(q.last().compareTo(new BigDecimal("0.001")) >= 0, q.ticker());
                if (q.upperLimit() != null) {
                    assertTrue(q.last().compareTo(q.upperLimit()) <= 0, q.ticker());
                }
                if (q.lowerLimit() != null) {
                    assertTrue(q.last().compareTo(q.lowerLimit()) >= 0, q.ticker());
                }
                long prev = lastVolume.getOrDefault(q.ticker(), 0L);
                assertTrue(q.volume() >= prev, q.ticker());
                lastVolume.put(q.ticker(), q.volume());
            }
        }
        assertEquals("SIMULATED", p.name());
    }
}
