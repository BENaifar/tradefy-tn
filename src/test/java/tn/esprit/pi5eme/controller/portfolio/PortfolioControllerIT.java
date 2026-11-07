package tn.esprit.pi5eme.controller.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.marketdata.InstrumentResponse;
import tn.esprit.pi5eme.dto.marketdata.QuoteResponse;
import tn.esprit.pi5eme.entity.Role;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.security.UserDetailsImpl;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortfolioControllerIT {

    @MockBean IMarketDataService marketData;
    @MockBean MarketDataProvider provider;
    @Autowired MockMvc mvc;

    private UserDetailsImpl principal() {
        return new UserDetailsImpl(21L, "Test", "User", "t@example.com", "x", Role.ROLE_USER, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @BeforeEach
    void mocks() {
        BigDecimal price = new BigDecimal("48.500");
        when(marketData.getInstrument(anyString())).thenAnswer(inv ->
                InstrumentResponse.builder().ticker(inv.getArgument(0)).name("TJARI").build());
        when(marketData.getLastPrice("TJARI")).thenReturn(price);
        when(marketData.getLastPrices(anyCollection())).thenReturn(Map.of("TJARI", price));
        when(marketData.getQuotes(anyCollection())).thenReturn(Map.of("TJARI",
                QuoteResponse.builder().ticker("TJARI").last(price).previousClose(price).build()));
        when(marketData.getInstruments(anyCollection())).thenReturn(Map.of("TJARI",
                InstrumentResponse.builder().ticker("TJARI").name("ATTIJARI").sector("Banques").build()));
    }

    @Test
    void summaryWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/portfolio")).andExpect(status().isUnauthorized());
    }

    @Test
    void summaryAuthenticated() throws Exception {
        mvc.perform(get("/api/portfolio").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cashBalance").value(100000.0));
    }

    @Test
    void withdrawZeroIs400() throws Exception {
        mvc.perform(post("/api/portfolio/withdraw").with(user(principal()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withdrawTooMuchIs422() throws Exception {
        mvc.perform(post("/api/portfolio/withdraw").with(user(principal()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":999999}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void devBuyShowsUpInPositions() throws Exception {
        mvc.perform(post("/api/portfolio/dev/execute").with(user(principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticker\":\"TJARI\",\"side\":\"BUY\",\"quantity\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(100));

        mvc.perform(get("/api/portfolio/positions").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].ticker").value("TJARI"));
    }

    @Test
    void devSellWithoutPositionIs422() throws Exception {
        mvc.perform(post("/api/portfolio/dev/execute").with(user(principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticker\":\"TJARI\",\"side\":\"SELL\",\"quantity\":5}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
