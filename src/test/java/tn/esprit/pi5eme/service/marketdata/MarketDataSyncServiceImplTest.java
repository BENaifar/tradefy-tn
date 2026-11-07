package tn.esprit.pi5eme.service.marketdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import tn.esprit.pi5eme.dto.marketdata.RawQuote;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.exception.MarketDataUnavailableException;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.repository.marketdata.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class MarketDataSyncServiceImplTest {

    @Autowired IMarketDataSyncService sync;
    @Autowired InstrumentRepository instruments;
    @Autowired QuoteRepository quotes;
    @Autowired IntradayTickRepository ticks;
    @Autowired PriceBarRepository bars;
    @Autowired WatchlistItemRepository watchlist;
    @MockBean MarketDataProvider provider;

    @BeforeEach
    void clean() {
        watchlist.deleteAll();
        bars.deleteAll();
        ticks.deleteAll();
        quotes.deleteAll();
        instruments.deleteAll();
        when(provider.name()).thenReturn("TEST");
    }

    private RawQuote raw(String ticker, String isin, String last) {
        return new RawQuote(ticker, isin, ticker + " NAME", null, "11",
                new BigDecimal(last), new BigDecimal("10.000"), null, new BigDecimal("11.000"), new BigDecimal("9.000"),
                new BigDecimal("1.0000"), 100, new BigDecimal("1000.000"),
                null, null, 0, 0, null, null, LocalDate.of(2026, 11, 6), LocalTime.of(15, 0));
    }

    private List<RawQuote> data(String p1, String p2) {
        return List.of(raw("AAA", "TN0000000001", p1), raw("BBB", "TN0000000002", p2));
    }

    @Test
    void firstRefreshCreatesEverything() {
        when(provider.fetchSnapshot()).thenReturn(data("10.500", "20.000"));
        IMarketDataSyncService.SyncResult r = sync.refresh();
        assertEquals(2, r.newInstruments());
        assertEquals(2, instruments.count());
        assertEquals(2, quotes.count());
        assertEquals(2, ticks.count());
        assertNotNull(sync.getLastSyncAt());
    }

    @Test
    void identicalRefreshAddsNoTicks() {
        when(provider.fetchSnapshot()).thenReturn(data("10.500", "20.000"));
        sync.refresh();
        IMarketDataSyncService.SyncResult r = sync.refresh();
        assertEquals(0, r.newInstruments());
        assertEquals(2, ticks.count());
    }

    @Test
    void changedPriceAddsTickAndUpdatesQuote() {
        when(provider.fetchSnapshot()).thenReturn(data("10.500", "20.000"));
        sync.refresh();
        when(provider.fetchSnapshot()).thenReturn(data("10.700", "20.000"));
        sync.refresh();
        assertEquals(3, ticks.count());
        assertEquals(0, new BigDecimal("10.700").compareTo(quotes.findByTicker("AAA").orElseThrow().getLast()));
    }

    @Test
    void adminSectorIsPreserved() {
        when(provider.fetchSnapshot()).thenReturn(data("10.500", "20.000"));
        sync.refresh();
        Instrument i = instruments.findByTickerIgnoreCase("AAA").orElseThrow();
        i.setSector("Banques");
        instruments.save(i);
        sync.refresh();
        assertEquals("Banques", instruments.findByTickerIgnoreCase("AAA").orElseThrow().getSector());
    }

    @Test
    void closeSessionIsIdempotent() {
        when(provider.fetchSnapshot()).thenReturn(data("10.500", "20.000"));
        sync.refresh();
        sync.closeSession();
        sync.closeSession();
        assertEquals(2, bars.count());
        assertEquals(0, new BigDecimal("10.500").compareTo(
                bars.findAll().stream().filter(b -> b.getClose().compareTo(new BigDecimal("10.5")) == 0)
                        .findFirst().orElseThrow().getClose()));
    }

    @Test
    void providerFailurePropagates() {
        when(provider.fetchSnapshot()).thenThrow(new MarketDataUnavailableException("down"));
        assertThrows(MarketDataUnavailableException.class, () -> sync.refresh());
    }
}
