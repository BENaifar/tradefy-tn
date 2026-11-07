package tn.esprit.pi5eme.service.marketdata;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.config.MarketDataProperties;
import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.CandleInterval;
import tn.esprit.pi5eme.dto.marketdata.CandleSeriesResponse;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.IntradayTick;
import tn.esprit.pi5eme.entity.marketdata.PriceBar;
import tn.esprit.pi5eme.entity.marketdata.Quote;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.exception.ResourceNotFoundException;
import tn.esprit.pi5eme.repository.marketdata.InstrumentRepository;
import tn.esprit.pi5eme.repository.marketdata.IntradayTickRepository;
import tn.esprit.pi5eme.repository.marketdata.PriceBarRepository;
import tn.esprit.pi5eme.repository.marketdata.QuoteRepository;
import tn.esprit.pi5eme.scheduler.MarketCalendar;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CandleServiceImpl implements ICandleService {

    static final int MAX_CANDLES = 5000;

    private final InstrumentRepository instrumentRepository;
    private final IntradayTickRepository tickRepository;
    private final PriceBarRepository priceBarRepository;
    private final QuoteRepository quoteRepository;
    private final CandleAggregator aggregator;
    private final MarketDataProperties props;
    private final MarketCalendar calendar;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public CandleSeriesResponse getSeries(String ticker, CandleInterval interval, LocalDate from, LocalDate to) {
        return build(ticker, interval, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Candle> getCandles(String ticker, CandleInterval interval, LocalDate from, LocalDate to) {
        return build(ticker, interval, from, to).getCandles();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Candle> getCandlesWithLookback(String ticker, CandleInterval interval, LocalDate from, LocalDate to,
                                               int extra) {
        LocalDate end = to != null ? to : calendar.today();
        LocalDate start = from != null ? from : defaultFrom(interval, end);
        if (extra <= 0) {
            return build(ticker, interval, start, end).getCandles();
        }
        double perCandleDays = switch (interval) {
            case D1 -> 1.5;
            case W1 -> 8;
            default -> 1;
        };
        long widen = Math.max(1, (long) Math.ceil(extra * perCandleDays));
        LocalDate cap = start.minusYears(2);
        long cutoff = start.atStartOfDay(MarketCalendar.ZONE).toEpochSecond();
        List<Candle> candles = List.of();
        for (int attempt = 0; attempt < 4; attempt++) {
            LocalDate widened = start.minusDays(widen);
            boolean capped = widened.isBefore(cap);
            if (capped) {
                widened = cap;
            }
            candles = build(ticker, interval, widened, end).getCandles();
            long before = candles.stream().filter(c -> c.time() < cutoff).count();
            if (before >= extra || capped) {
                break;
            }
            widen *= 2;
        }
        return candles;
    }

    @Override
    public LocalDate defaultFrom(CandleInterval interval, LocalDate to) {
        return switch (interval) {
            case M5 -> to.minusDays(2);
            case M15, M30 -> to.minusDays(7);
            case H1, H2 -> to.minusMonths(1);
            case H4 -> to.minusMonths(3);
            case D1 -> to.minusYears(1);
            case W1 -> to.minusYears(5);
        };
    }

    private CandleSeriesResponse build(String ticker, CandleInterval interval, LocalDate from, LocalDate to) {
        Instrument instrument = instrumentRepository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found: " + ticker));
        CandleInterval iv = interval != null ? interval : CandleInterval.D1;
        LocalDate today = calendar.today();
        LocalDate end = to != null ? to : today;
        LocalDate start = from != null ? from : defaultFrom(iv, end);
        if (start.isAfter(end)) {
            throw new BadRequestException("'from' must not be after 'to'");
        }

        List<Candle> candles;
        boolean live = false;
        if (iv.isIntraday()) {
            LocalDate oldest = today.minusDays(props.getIntradayRetentionDays());
            if (start.isBefore(oldest)) {
                start = oldest;
            }
            Instant fromI = start.atStartOfDay(MarketCalendar.ZONE).toInstant();
            Instant toI = end.plusDays(1).atStartOfDay(MarketCalendar.ZONE).toInstant();
            List<IntradayTick> ticks = tickRepository.findRange(instrument, fromI, toI);
            candles = aggregator.fromTicks(ticks, iv, props.getSessionOpen(), props.getSessionClose(),
                    props.isIgnoreMarketHours());
        } else {
            // weekly candles need the whole first week, so start on its Monday
            LocalDate barsFrom = iv == CandleInterval.W1 ? start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : start;
            List<PriceBar> bars = priceBarRepository
                    .findByInstrument_TickerIgnoreCaseAndDateBetweenOrderByDateAsc(ticker, barsFrom, end);
            candles = new ArrayList<>(aggregator.fromDailyBars(bars));
            if (!end.isBefore(today) && bars.stream().noneMatch(b -> b.getDate().equals(today))) {
                Optional<Candle> liveCandle = liveCandle(instrument, today);
                if (liveCandle.isPresent()) {
                    candles.add(liveCandle.get());
                    live = true;
                }
            }
            if (iv == CandleInterval.W1) {
                candles = new ArrayList<>(aggregator.toWeekly(candles));
            }
        }

        boolean truncated = false;
        if (candles.size() > MAX_CANDLES) {
            candles = new ArrayList<>(candles.subList(candles.size() - MAX_CANDLES, candles.size()));
            truncated = true;
        }
        return CandleSeriesResponse.builder().ticker(instrument.getTicker()).interval(iv).from(start).to(end)
                .timezone(MarketCalendar.ZONE.getId()).live(live).truncated(truncated).candles(candles).build();
    }

    /** Today's in-progress candle, built from the quote, once the trading day has started. */
    private Optional<Candle> liveCandle(Instrument instrument, LocalDate today) {
        if (!calendar.isTradingDay(today)) {
            return Optional.empty();
        }
        LocalTime now = LocalTime.now(clock.withZone(MarketCalendar.ZONE));
        if (!props.isIgnoreMarketHours() && now.isBefore(props.getSessionOpen())) {
            return Optional.empty();
        }
        Quote q = quoteRepository.findByInstrument(instrument).orElse(null);
        if (q == null || q.getLast() == null || !today.equals(q.getSessionDate())) {
            return Optional.empty();
        }
        BigDecimal open = q.getOpen();
        if (open == null) {
            Instant dayStart = today.atStartOfDay(MarketCalendar.ZONE).toInstant();
            List<IntradayTick> ticks = tickRepository.findFromForInstrument(instrument, dayStart);
            open = ticks.isEmpty() ? q.getPreviousClose() : ticks.get(0).getPrice();
        }
        if (open == null) {
            open = q.getLast();
        }
        BigDecimal high = (q.getHigh() != null ? q.getHigh() : q.getLast()).max(open).max(q.getLast());
        BigDecimal low = (q.getLow() != null ? q.getLow() : q.getLast()).min(open).min(q.getLast());
        return Optional.of(new Candle(today.atStartOfDay(MarketCalendar.ZONE).toEpochSecond(),
                open, high, low, q.getLast(), q.getVolume()));
    }
}
