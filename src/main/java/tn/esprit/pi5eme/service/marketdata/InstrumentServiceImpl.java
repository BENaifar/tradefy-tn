package tn.esprit.pi5eme.service.marketdata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.marketdata.InstrumentResponse;
import tn.esprit.pi5eme.dto.marketdata.InstrumentUpdateRequest;
import tn.esprit.pi5eme.dto.marketdata.MoverType;
import tn.esprit.pi5eme.dto.marketdata.QuoteResponse;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.Quote;
import tn.esprit.pi5eme.exception.ResourceNotFoundException;
import tn.esprit.pi5eme.repository.marketdata.InstrumentRepository;
import tn.esprit.pi5eme.repository.marketdata.QuoteRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstrumentServiceImpl implements IInstrumentService {

    private static final int MAX_LIMIT = 50;

    private final InstrumentRepository instrumentRepository;
    private final QuoteRepository quoteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InstrumentResponse> search(String q, String sector) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        String sec = (sector == null || sector.isBlank()) ? null : sector.trim();
        Map<String, QuoteResponse> quotes = quotesByTicker();
        return instrumentRepository.search(query, sec).stream()
                .map(i -> InstrumentResponse.fromEntity(i, quotes.get(i.getTicker().toUpperCase(Locale.ROOT))))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InstrumentResponse getByTicker(String ticker) {
        Instrument i = find(ticker);
        QuoteResponse quote = quoteRepository.findByInstrument(i).map(QuoteResponse::fromEntity).orElse(null);
        return InstrumentResponse.fromEntity(i, quote);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSectors() {
        return instrumentRepository.findDistinctSectors();
    }

    @Override
    @Transactional
    public InstrumentResponse update(String ticker, InstrumentUpdateRequest req) {
        Instrument i = find(ticker);
        if (req.getSector() != null) {
            i.setSector(req.getSector());
        }
        if (req.getActive() != null) {
            i.setActive(req.getActive());
        }
        Instrument saved = instrumentRepository.save(i);
        log.info("Instrument {} updated (sector={}, active={})", saved.getTicker(), saved.getSector(), saved.isActive());
        return InstrumentResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteResponse> getMovers(MoverType type, int limit) {
        int effective = Math.max(1, Math.min(MAX_LIMIT, limit));
        List<Quote> quotes = quoteRepository.findAllActiveWithInstrument();
        Comparator<Quote> byChange = Comparator.comparing(
                (Quote q) -> q.getChangePercent() == null ? BigDecimal.ZERO : q.getChangePercent());
        Comparator<Quote> byTurnover = Comparator.comparing(
                (Quote q) -> q.getTurnover() == null ? BigDecimal.ZERO : q.getTurnover());
        return switch (type) {
            case GAINERS -> quotes.stream().filter(q -> q.getVolume() > 0)
                    .sorted(byChange.reversed()).limit(effective).map(QuoteResponse::fromEntity).toList();
            case LOSERS -> quotes.stream().filter(q -> q.getVolume() > 0)
                    .sorted(byChange).limit(effective).map(QuoteResponse::fromEntity).toList();
            case ACTIVE -> quotes.stream()
                    .sorted(byTurnover.reversed()).limit(effective).map(QuoteResponse::fromEntity).toList();
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteResponse> getAllQuotes() {
        return quoteRepository.findAllActiveWithInstrument().stream()
                .sorted(Comparator.comparing((Quote q) -> q.getInstrument().getTicker()))
                .map(QuoteResponse::fromEntity).toList();
    }

    private Instrument find(String ticker) {
        return instrumentRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found: " + ticker));
    }

    private Map<String, QuoteResponse> quotesByTicker() {
        Map<String, QuoteResponse> map = new HashMap<>();
        for (Quote q : quoteRepository.findAllActiveWithInstrument()) {
            map.put(q.getInstrument().getTicker().toUpperCase(Locale.ROOT), QuoteResponse.fromEntity(q));
        }
        return map;
    }
}
