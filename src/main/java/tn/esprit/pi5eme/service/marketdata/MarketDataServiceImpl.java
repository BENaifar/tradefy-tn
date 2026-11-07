package tn.esprit.pi5eme.service.marketdata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.marketdata.InstrumentResponse;
import tn.esprit.pi5eme.dto.marketdata.QuoteResponse;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.Quote;
import tn.esprit.pi5eme.exception.MarketDataUnavailableException;
import tn.esprit.pi5eme.exception.ResourceNotFoundException;
import tn.esprit.pi5eme.repository.marketdata.InstrumentRepository;
import tn.esprit.pi5eme.repository.marketdata.QuoteRepository;
import tn.esprit.pi5eme.scheduler.MarketCalendar;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataServiceImpl implements IMarketDataService {

    private final InstrumentRepository instrumentRepository;
    private final QuoteRepository quoteRepository;
    private final MarketCalendar calendar;

    @Override
    @Transactional(readOnly = true)
    public QuoteResponse getQuote(String ticker) {
        Instrument instrument = instrumentRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found: " + ticker));
        Quote quote = quoteRepository.findByInstrument(instrument)
                .orElseThrow(() -> new MarketDataUnavailableException("No quote available yet for " + ticker));
        return QuoteResponse.fromEntity(quote);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getLastPrice(String ticker) {
        BigDecimal last = getQuote(ticker).getLast();
        if (last == null) {
            throw new MarketDataUnavailableException("No price available yet for " + ticker);
        }
        return last;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getLastPrices(Collection<String> tickers) {
        Map<String, BigDecimal> result = new HashMap<>();
        getQuotes(tickers).forEach((t, q) -> {
            if (q.getLast() != null) {
                result.put(t, q.getLast());
            }
        });
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, QuoteResponse> getQuotes(Collection<String> tickers) {
        Map<String, QuoteResponse> result = new HashMap<>();
        Set<String> upper = upperCase(tickers);
        if (upper.isEmpty()) {
            return result;
        }
        for (Quote q : quoteRepository.findByTickers(upper)) {
            result.put(q.getInstrument().getTicker().toUpperCase(Locale.ROOT), QuoteResponse.fromEntity(q));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public InstrumentResponse getInstrument(String ticker) {
        return instrumentRepository.findByTickerIgnoreCase(ticker)
                .map(InstrumentResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found: " + ticker));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, InstrumentResponse> getInstruments(Collection<String> tickers) {
        Set<String> upper = upperCase(tickers);
        Map<String, InstrumentResponse> result = new HashMap<>();
        if (upper.isEmpty()) {
            return result;
        }
        for (Instrument i : instrumentRepository.findAll()) {
            String key = i.getTicker().toUpperCase(Locale.ROOT);
            if (upper.contains(key)) {
                result.put(key, InstrumentResponse.fromEntity(i));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<String> getSectors() {
        return instrumentRepository.findDistinctSectors();
    }

    @Override
    public boolean isMarketOpen() {
        return calendar.isOpenNow();
    }

    private static Set<String> upperCase(Collection<String> tickers) {
        Set<String> upper = new HashSet<>();
        if (tickers != null) {
            for (String t : tickers) {
                if (t != null && !t.isBlank()) {
                    upper.add(t.trim().toUpperCase(Locale.ROOT));
                }
            }
        }
        return upper;
    }
}
