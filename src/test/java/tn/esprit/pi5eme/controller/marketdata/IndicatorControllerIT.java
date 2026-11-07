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

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IndicatorControllerIT {

    @MockBean MarketDataProvider provider;
    @Autowired MockMvc mvc;
    @Autowired InstrumentRepository instruments;

    @BeforeEach
    void seed() {
        instruments.save(Instrument.builder().ticker("IND").isin("TNIND0000001").name("IND").build());
    }

    @Test
    void catalogWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/market/indicators")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void catalogListsTheFiveIndicators() throws Exception {
        mvc.perform(get("/api/market/indicators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].code").value(hasItems("SMA", "EMA", "BOLL", "RSI", "MACD")));
    }

    @Test
    @WithMockUser
    void smaReturnsSeries() throws Exception {
        mvc.perform(get("/api/market/instruments/IND/indicators/SMA?interval=D1&period=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("SMA"))
                .andExpect(jsonPath("$.data.series.value").isArray());
    }

    @Test
    @WithMockUser
    void macdReturnsThreeSeries() throws Exception {
        mvc.perform(get("/api/market/instruments/IND/indicators/MACD?interval=H1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.series.macd").isArray())
                .andExpect(jsonPath("$.data.series.signal").isArray())
                .andExpect(jsonPath("$.data.series.histogram").isArray());
    }

    @Test
    @WithMockUser
    void rsiPeriodOneIs400() throws Exception {
        mvc.perform(get("/api/market/instruments/IND/indicators/RSI?interval=H1&period=1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void unknownIndicatorIs404() throws Exception {
        mvc.perform(get("/api/market/instruments/IND/indicators/NOPE")).andExpect(status().isNotFound());
    }
}
