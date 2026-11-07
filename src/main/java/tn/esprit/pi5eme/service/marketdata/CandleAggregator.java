package tn.esprit.pi5eme.service.marketdata;

import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.CandleInterval;
import tn.esprit.pi5eme.entity.marketdata.IntradayTick;
import tn.esprit.pi5eme.entity.marketdata.PriceBar;
import tn.esprit.pi5eme.scheduler.MarketCalendar;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Pure candle maths. No repositories. */
@Component
public class CandleAggregator {

    /** Ticks up to this long after the session close still belong to the last candle of the day. */
    static final java.time.Duration CLOSE_GRACE = java.time.Duration.ofMinutes(10);

    private static final class Bucket {
        BigDecimal open;
        BigDecimal high;
        BigDecimal low;
        BigDecimal close;
        long lastCumVolume;
    }

    /**
     * Builds intraday candles from ticks (sorted ascending). Buckets are aligned to the session open
     * (or to midnight when ignoreMarketHours is set) and never span two days. Empty buckets between the
     * first and last tick of a day are forward-filled with a flat candle.
     */
    public List<Candle> fromTicks(List<IntradayTick> ticks, CandleInterval interval, LocalTime sessionOpen,
                                  LocalTime sessionClose, boolean ignoreMarketHours) {
        if (interval == null || !interval.isIntraday()) {
            throw new IllegalArgumentException("Not an intraday interval: " + interval);
        }
        long bucketSeconds = interval.bucket().getSeconds();
        Map<LocalDate, TreeMap<Long, Bucket>> days = new TreeMap<>();

        for (IntradayTick t : ticks) {
            ZonedDateTime local = t.getTimestamp().atZone(MarketCalendar.ZONE);
            LocalTime time = local.toLocalTime();
            // The end-of-session refresh runs at 14:15, after the 14:10 close: it carries the official
            // closing price and volume, so ticks inside the grace window are folded into the last bucket.
            boolean closingTick = !ignoreMarketHours && time.isAfter(sessionClose)
                    && !time.isAfter(sessionClose.plus(CLOSE_GRACE));
            if (!ignoreMarketHours && !closingTick && (time.isBefore(sessionOpen) || time.isAfter(sessionClose))) {
                continue;
            }
            ZonedDateTime dayStart = ignoreMarketHours
                    ? local.toLocalDate().atStartOfDay(MarketCalendar.ZONE)
                    : local.toLocalDate().atTime(sessionOpen).atZone(MarketCalendar.ZONE);
            long seconds = closingTick
                    ? local.toLocalDate().atTime(sessionClose).atZone(MarketCalendar.ZONE).toEpochSecond() - 1
                    : local.toEpochSecond();
            long index = (seconds - dayStart.toEpochSecond()) / bucketSeconds;
            BigDecimal price = MoneyUtils.money(t.getPrice());
            Bucket b = days.computeIfAbsent(local.toLocalDate(), d -> new TreeMap<>())
                    .computeIfAbsent(index, i -> new Bucket());
            if (b.open == null) {
                b.open = price;
                b.high = price;
                b.low = price;
            }
            b.high = b.high.max(price);
            b.low = b.low.min(price);
            b.close = price;
            b.lastCumVolume = t.getVolume();
        }

        List<Candle> result = new ArrayList<>();
        for (Map.Entry<LocalDate, TreeMap<Long, Bucket>> day : days.entrySet()) {
            ZonedDateTime dayStart = ignoreMarketHours
                    ? day.getKey().atStartOfDay(MarketCalendar.ZONE)
                    : day.getKey().atTime(sessionOpen).atZone(MarketCalendar.ZONE);
            TreeMap<Long, Bucket> buckets = day.getValue();
            long prevCum = 0;
            BigDecimal prevClose = null;
            for (long idx = buckets.firstKey(); idx <= buckets.lastKey(); idx++) {
                long time = dayStart.toEpochSecond() + idx * bucketSeconds;
                Bucket b = buckets.get(idx);
                if (b == null) {
                    result.add(new Candle(time, prevClose, prevClose, prevClose, prevClose, 0));
                } else {
                    long volume = Math.max(0, b.lastCumVolume - prevCum);
                    prevCum = b.lastCumVolume;
                    prevClose = b.close;
                    result.add(new Candle(time, b.open, b.high, b.low, b.close, volume));
                }
            }
        }
        return result;
    }

    /** One bar becomes one candle; time is the session date at 00:00 Africa/Tunis. */
    public List<Candle> fromDailyBars(List<PriceBar> bars) {
        List<Candle> result = new ArrayList<>();
        for (PriceBar b : bars) {
            result.add(new Candle(b.getDate().atStartOfDay(MarketCalendar.ZONE).toEpochSecond(),
                    b.getOpen(), b.getHigh(), b.getLow(), b.getClose(), b.getVolume()));
        }
        return result;
    }

    /** Groups daily candles by ISO week (Monday start). Time is the first trading day of the week. */
    public List<Candle> toWeekly(List<Candle> daily) {
        Map<String, List<Candle>> weeks = new java.util.LinkedHashMap<>();
        for (Candle c : daily) {
            LocalDate d = java.time.Instant.ofEpochSecond(c.time()).atZone(MarketCalendar.ZONE).toLocalDate();
            String key = d.get(IsoFields.WEEK_BASED_YEAR) + "-" + d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            weeks.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }
        List<Candle> result = new ArrayList<>();
        for (List<Candle> week : weeks.values()) {
            Candle first = week.get(0);
            Candle last = week.get(week.size() - 1);
            BigDecimal high = first.high();
            BigDecimal low = first.low();
            long volume = 0;
            for (Candle c : week) {
                high = high.max(c.high());
                low = low.min(c.low());
                volume += c.volume();
            }
            result.add(new Candle(first.time(), first.open(), high, low, last.close(), volume));
        }
        return result;
    }
}
