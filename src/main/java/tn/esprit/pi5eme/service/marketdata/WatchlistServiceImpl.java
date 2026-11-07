package tn.esprit.pi5eme.service.marketdata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.marketdata.QuoteResponse;
import tn.esprit.pi5eme.dto.marketdata.WatchlistItemResponse;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.WatchlistItem;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.exception.ResourceNotFoundException;
import tn.esprit.pi5eme.repository.marketdata.InstrumentRepository;
import tn.esprit.pi5eme.repository.marketdata.QuoteRepository;
import tn.esprit.pi5eme.repository.marketdata.WatchlistItemRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchlistServiceImpl implements IWatchlistService {

    private final WatchlistItemRepository watchlistRepository;
    private final InstrumentRepository instrumentRepository;
    private final QuoteRepository quoteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WatchlistItemResponse> list(Long userId) {
        List<WatchlistItem> items = watchlistRepository.findByUserIdOrderByAddedAtAsc(userId);
        if (items.isEmpty()) {
            return List.of();
        }
        List<String> tickers = items.stream()
                .map(w -> w.getInstrument().getTicker().toUpperCase(Locale.ROOT)).toList();
        Map<String, QuoteResponse> quotes = quoteRepository.findByTickers(tickers).stream()
                .collect(Collectors.toMap(q -> q.getInstrument().getTicker().toUpperCase(Locale.ROOT),
                        QuoteResponse::fromEntity));
        return items.stream()
                .map(w -> WatchlistItemResponse.fromEntity(w,
                        quotes.get(w.getInstrument().getTicker().toUpperCase(Locale.ROOT))))
                .toList();
    }

    @Override
    @Transactional
    public WatchlistItemResponse add(Long userId, String ticker) {
        Instrument instrument = instrumentRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found: " + ticker));
        if (watchlistRepository.existsByUserIdAndInstrument(userId, instrument)) {
            throw new BadRequestException("Instrument " + instrument.getTicker() + " is already in watchlist");
        }
        WatchlistItem saved = watchlistRepository.save(
                WatchlistItem.builder().userId(userId).instrument(instrument).build());
        log.info("User {} added {} to watchlist", userId, instrument.getTicker());
        QuoteResponse quote = quoteRepository.findByInstrument(instrument).map(QuoteResponse::fromEntity).orElse(null);
        return WatchlistItemResponse.fromEntity(saved, quote);
    }

    @Override
    @Transactional
    public void remove(Long userId, String ticker) {
        WatchlistItem item = watchlistRepository.findByUserIdAndInstrument_TickerIgnoreCase(userId, ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not in watchlist: " + ticker));
        watchlistRepository.delete(item);
        log.info("User {} removed {} from watchlist", userId, ticker);
    }
}
