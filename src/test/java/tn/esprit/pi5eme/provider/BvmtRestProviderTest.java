package tn.esprit.pi5eme.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tn.esprit.pi5eme.config.MarketDataProperties;
import tn.esprit.pi5eme.exception.MarketDataUnavailableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BvmtRestProviderTest {

    private MockRestServiceServer server;
    private BvmtRestProvider provider;
    private final MarketDataProperties props = new MarketDataProperties();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new BvmtRestProvider(builder.build(), props, new BvmtQuoteMapper());
    }

    @Test
    void serverErrorBecomesUnavailable() {
        server.expect(requestTo(props.getBvmtUrl())).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThrows(MarketDataUnavailableException.class, provider::fetchSnapshot);
    }

    @Test
    void validBodyGivesFourQuotes() {
        server.expect(requestTo(props.getBvmtUrl()))
                .andRespond(withSuccess(new ClassPathResource("bvmt/market.json"), MediaType.APPLICATION_JSON));
        assertEquals(4, provider.fetchSnapshot().size());
    }
}
