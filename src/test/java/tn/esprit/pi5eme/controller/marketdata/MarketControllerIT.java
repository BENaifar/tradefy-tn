package tn.esprit.pi5eme.controller.marketdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.Quote;
import tn.esprit.pi5eme.repository.marketdata.*;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MarketControllerIT {

    @MockBean MarketDataProvider provider;
    @Autowired MockMvc mvc;
    @Autowired InstrumentRepository instruments;
    @Autowired QuoteRepository quotes;
    @Autowired IntradayTickRepository ticks;
    @Autowired PriceBarRepository bars;
    @Autowired WatchlistItemRepository watchlist;

    @BeforeEach
    void seed() {
        watchlist.deleteAll();
        bars.deleteAll();
        ticks.deleteAll();
        quotes.deleteAll();
        instruments.deleteAll();
        for (String t : new String[]{"AB", "ATB"}) {
            Instrument i = instruments.save(Instrument.builder().ticker(t).isin("TN" + t).name(t + " BANK").build());
            quotes.save(Quote.builder().instrument(i).last(BigDecimal.TEN).previousClose(BigDecimal.TEN)
                    .changePercent(BigDecimal.ONE).volume(5).turnover(BigDecimal.TEN).build());
        }
    }

    @Test
    void quotesWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/market/quotes")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void quotesAuthenticated() throws Exception {
        mvc.perform(get("/api/market/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser
    void unknownQuoteIs404() throws Exception {
        mvc.perform(get("/api/market/quotes/NOPE")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateAsUserIs403() throws Exception {
        mvc.perform(put("/api/market/instruments/AB").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sector\":\"Banques\"}")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAsAdminIs200() throws Exception {
        mvc.perform(put("/api/market/instruments/AB").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sector\":\"Banques\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sector").value("Banques"));
    }
}
