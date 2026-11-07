package tn.esprit.pi5eme.service.marketdata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.config.MarketDataProperties;
import tn.esprit.pi5eme.dto.marketdata.QuoteResponse;
import tn.esprit.pi5eme.dto.marketdata.RawQuote;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.IntradayTick;
import tn.esprit.pi5eme.entity.marketdata.PriceBar;
import tn.esprit.pi5eme.entity.marketdata.Quote;
import tn.esprit.pi5eme.event.QuotesUpdatedEvent;
import tn.esprit.pi5eme.event.SessionClosedEvent;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.repository.marketdata.InstrumentRepository;
import tn.esprit.pi5eme.repository.marketdata.IntradayTickRepository;
import tn.esprit.pi5eme.repository.marketdata.PriceBarRepository;
import tn.esprit.pi5eme.repository.marketdata.QuoteRepository;
import tn.esprit.pi5eme.scheduler.MarketCalendar;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataSyncServiceImpl implements IMarketDataSyncService {

    private final MarketDataProvider provider;
    private final InstrumentRepository instrumentRepository;
    private final QuoteRepository quoteRepository;
    private final IntradayTickRepository tickRepository;
    private final PriceBarRepository priceBarRepository;
    private final MarketDataProperties props;
    private final MarketCalendar calendar;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    private volatile Instant lastSyncAt;

    @Override
    @Transactional
    public SyncResult refresh() {
        List<RawQuote> raws = provider.fetchSnapshot();
        Instant now = Instant.now(clock);

        Map<String, Instrument> instruments = instrumentRepository.findAll().stream()
                .collect(Collectors.toMap(i -> i.getTicker().toUpperCase(), Function.identity()));
        Map<Long, Quote> quotes = new HashMap<>();
        for (Quote q : quoteRepository.findAll()) {
            quotes.put(q.getInstrument().getId(), q);
        }

        int newInstruments = 0;
        List<Quote> toSave = new ArrayList<>();
        List<IntradayTick> ticks = new ArrayList<>();

        for (RawQuote raw : raws) {
            Instrument instrument = instruments.get(raw.ticker());
            if (instrument == null) {
                instrument = instrumentRepository.save(Instrument.builder()
                        .ticker(raw.ticker())
                        .isin(raw.isin() != null ? raw.isin() : raw.ticker())
                        .name(raw.name() != null ? raw.name() : raw.ticker())
                        .arabName(raw.arabName())
                        .valGroup(raw.valGroup())
                        .active(true)
                        .build());
                instruments.put(raw.ticker(), instrument);
                newInstruments++;
            } else {
                // Only name and arabName; sector and active belong to the admin.
                boolean changed = false;
                if (raw.name() != null && !raw.name().equals(instrument.getName())) {
                    instrument.setName(raw.name());
                    changed = true;
                }
                if (raw.arabName() != null && !raw.arabName().equals(instrument.getArabName())) {
                    instrument.setArabName(raw.arabName());
                    changed = true;
                }
                if (changed) {
                    instrumentRepository.save(instrument);
                }
            }

            Quote quote = quotes.get(instrument.getId());
            if (quote == null) {
                quote = new Quote();
                quote.setInstrument(instrument);
                quotes.put(instrument.getId(), quote);
            }
            quote.setLast(raw.last());
            quote.setPreviousClose(raw.previousClose());
            quote.setOpen(raw.open());
            quote.setHigh(raw.high());
            quote.setLow(raw.low());
            quote.setChangePercent(raw.changePercent());
            quote.setVolume(raw.volume());
            quote.setTurnover(raw.turnover());
            quote.setBid(raw.bid());
            quote.setAsk(raw.ask());
            quote.setBidQty(raw.bidQty());
            quote.setAskQty(raw.askQty());
            quote.setUpperLimit(raw.upperLimit());
            quote.setLowerLimit(raw.lowerLimit());
            quote.setSessionDate(raw.sessionDate());
            quote.setQuoteTime(raw.quoteTime());
            quote.setUpdatedAt(now);
            toSave.add(quote);

            IntradayTick lastTick = tickRepository.findTopByInstrumentOrderByTimestampDesc(instrument).orElse(null);
            if (lastTick == null
                    || lastTick.getPrice().compareTo(raw.last()) != 0
                    || lastTick.getVolume() != raw.volume()) {
                ticks.add(IntradayTick.builder()
                        .instrument(instrument)
                        .timestamp(now)
                        .price(raw.last())
                        .volume(raw.volume())
                        .build());
            }
        }

        List<Quote> saved = quoteRepository.saveAll(toSave);
        tickRepository.saveAll(ticks);
        lastSyncAt = now;

        List<QuoteResponse> responses = saved.stream().map(QuoteResponse::fromEntity).toList();
        publisher.publishEvent(new QuotesUpdatedEvent(responses, now));

        log.info("Market sync [{}]: {} quotes, {} new instruments", provider.name(), raws.size(), newInstruments);
        return new SyncResult(raws.size(), newInstruments, raws.size() - newInstruments, provider.name(), now);
    }

    @Override
    @Transactional
    public void closeSession() {
        List<Quote> quotes = quoteRepository.findAllActiveWithInstrument();
        LocalDate sessionDate = quotes.stream()
                .map(Quote::getSessionDate)
                .filter(d -> d != null)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(calendar.today());
        Instant dayStart = sessionDate.atStartOfDay(MarketCalendar.ZONE).toInstant();

        int count = 0;
        for (Quote q : quotes) {
            if (q.getLast() == null && q.getVolume() <= 0) {
                continue;
            }
            Instrument instrument = q.getInstrument();
            PriceBar bar = priceBarRepository.findByInstrumentAndDate(instrument, sessionDate)
                    .orElseGet(() -> PriceBar.builder().instrument(instrument).date(sessionDate).build());
            bar.setClose(q.getLast());
            bar.setHigh(q.getHigh() != null ? q.getHigh() : q.getLast());
            bar.setLow(q.getLow() != null ? q.getLow() : q.getLast());
            bar.setVolume(q.getVolume());
            if (q.getOpen() != null) {
                bar.setOpen(q.getOpen());
            } else {
                List<IntradayTick> dayTicks = tickRepository.findFromForInstrument(instrument, dayStart);
                if (!dayTicks.isEmpty()) {
                    bar.setOpen(dayTicks.get(0).getPrice());
                } else {
                    bar.setOpen(q.getPreviousClose());
                }
            }
            priceBarRepository.save(bar);
            count++;
        }
        log.info("Session closed for {}: {} bars written", sessionDate, count);
        publisher.publishEvent(new SessionClosedEvent(sessionDate));
    }

    @Override
    @Transactional
    public int purgeOldTicks() {
        Instant before = Instant.now(clock).minus(Duration.ofDays(props.getIntradayRetentionDays()));
        int deleted = tickRepository.deleteOlderThan(before);
        log.info("Purged {} intraday ticks older than {}", deleted, before);
        return deleted;
    }

    @Override
    public Instant getLastSyncAt() {
        return lastSyncAt;
    }
}
