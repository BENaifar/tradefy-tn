package tn.esprit.pi5eme.service.marketdata;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tn.esprit.pi5eme.event.QuotesUpdatedEvent;

@Component
@RequiredArgsConstructor
public class QuoteBroadcaster {

    private final SimpMessagingTemplate template;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(QuotesUpdatedEvent e) {
        template.convertAndSend("/topic/quotes", e.quotes());
        e.quotes().forEach(q -> template.convertAndSend("/topic/quotes/" + q.getTicker(), q));
    }
}
