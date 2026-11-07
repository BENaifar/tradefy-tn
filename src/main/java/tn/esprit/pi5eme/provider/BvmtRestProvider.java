package tn.esprit.pi5eme.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.esprit.pi5eme.config.MarketDataProperties;
import tn.esprit.pi5eme.dto.marketdata.RawQuote;
import tn.esprit.pi5eme.exception.MarketDataUnavailableException;
import tn.esprit.pi5eme.scheduler.MarketCalendar;

import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.marketdata.provider", havingValue = "bvmt", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class BvmtRestProvider implements MarketDataProvider {

    @Qualifier("bvmtRestClient")
    private final RestClient restClient;
    private final MarketDataProperties props;
    private final BvmtQuoteMapper mapper;

    @Override
    public List<RawQuote> fetchSnapshot() {
        BvmtMarketResponse body;
        try {
            body = restClient.get().uri(props.getBvmtUrl()).retrieve().body(BvmtMarketResponse.class);
        } catch (RestClientException e) {
            throw new MarketDataUnavailableException("BVMT request failed: " + e.getMessage());
        }
        if (body == null || body.getMarkets() == null || body.getMarkets().isEmpty()) {
            throw new MarketDataUnavailableException("BVMT returned an empty response");
        }
        List<RawQuote> quotes = mapper.map(body, LocalDate.now(MarketCalendar.ZONE));
        if (quotes.isEmpty()) {
            throw new MarketDataUnavailableException("BVMT response contained no usable quotes");
        }
        return quotes;
    }

    @Override
    public String name() {
        return "BVMT";
    }
}
