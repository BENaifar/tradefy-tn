package tn.esprit.pi5eme.service.marketdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.exception.ResourceNotFoundException;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.repository.marketdata.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WatchlistServiceImplTest {

    @MockBean MarketDataProvider provider;
    @Autowired IWatchlistService service;
    @Autowired InstrumentRepository instruments;
    @Autowired WatchlistItemRepository watchlist;
    @Autowired QuoteRepository quotes;
    @Autowired IntradayTickRepository ticks;
    @Autowired PriceBarRepository bars;

    @BeforeEach
    void clean() {
        watchlist.deleteAll();
        bars.deleteAll();
        ticks.deleteAll();
        quotes.deleteAll();
        instruments.deleteAll();
        instruments.save(Instrument.builder().ticker("AB").isin("TN0000000001").name("AMEN BANK").build());
    }

    @Test
    void addThenList() {
        service.add(1L, "ab");
        assertEquals(1, service.list(1L).size());
        assertNull(service.list(1L).get(0).getQuote());
    }

    @Test
    void addingTwiceFails() {
        service.add(1L, "AB");
        assertThrows(BadRequestException.class, () -> service.add(1L, "AB"));
    }

    @Test
    void addUnknownTickerIs404() {
        assertThrows(ResourceNotFoundException.class, () -> service.add(1L, "NOPE"));
    }

    @Test
    void removeUnknownItemIs404() {
        assertThrows(ResourceNotFoundException.class, () -> service.remove(1L, "AB"));
    }

    @Test
    void usersAreIsolated() {
        service.add(1L, "AB");
        assertEquals(0, service.list(2L).size());
        assertThrows(ResourceNotFoundException.class, () -> service.remove(2L, "AB"));
    }
}
