package tn.esprit.pi5eme.controller.marketdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.repository.marketdata.InstrumentRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CandleControllerIT {

    @MockBean MarketDataProvider provider;
    @Autowired MockMvc mvc;
    @Autowired InstrumentRepository instruments;

    @BeforeEach
    void seed() {
        instruments.save(Instrument.builder().ticker("CTL").isin("TNCTL0000001").name("CTL").build());
    }

    @Test
    @WithMockUser
    void candlesReturnArray() throws Exception {
        mvc.perform(get("/api/market/instruments/CTL/candles?interval=H1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candles").isArray())
                .andExpect(jsonPath("$.data.interval").value("H1"));
    }

    @Test
    @WithMockUser
    void invalidIntervalIs400() throws Exception {
        mvc.perform(get("/api/market/instruments/CTL/candles?interval=H3"))
                .andExpect(status().isBadRequest());
    }
}
