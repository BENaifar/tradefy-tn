package tn.esprit.pi5eme.service.marketdata;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tn.esprit.pi5eme.dto.marketdata.QuoteResponse;
import tn.esprit.pi5eme.event.QuotesUpdatedEvent;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class QuoteBroadcasterTest {

    @Test
    void sendsListPlusOnePerTicker() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        QuoteBroadcaster b = new QuoteBroadcaster(template);
        List<QuoteResponse> quotes = List.of(
                QuoteResponse.builder().ticker("AB").build(),
                QuoteResponse.builder().ticker("AL").build());
        b.on(new QuotesUpdatedEvent(quotes, Instant.now()));
        verify(template, times(3)).convertAndSend(anyString(), any(Object.class));
        verify(template).convertAndSend("/topic/quotes", quotes);
        verify(template).convertAndSend("/topic/quotes/AB", quotes.get(0));
        verify(template).convertAndSend("/topic/quotes/AL", quotes.get(1));
    }
}
