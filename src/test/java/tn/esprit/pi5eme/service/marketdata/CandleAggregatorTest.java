package tn.esprit.pi5eme.service.marketdata;

import org.junit.jupiter.api.Test;
import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.CandleInterval;
import tn.esprit.pi5eme.entity.marketdata.IntradayTick;
import tn.esprit.pi5eme.scheduler.MarketCalendar;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CandleAggregatorTest {

    private static final LocalTime OPEN = LocalTime.of(9, 0);
    private static final LocalTime CLOSE = LocalTime.of(14, 10);
    private static final LocalDate DAY = LocalDate.of(2026, 11, 6);   // a Friday

    private final CandleAggregator agg = new CandleAggregator();

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    private IntradayTick tick(LocalDate day, int hour, int minute, String price, long cumVolume) {
        Instant at = ZonedDateTime.of(day, LocalTime.of(hour, minute), MarketCalendar.ZONE).toInstant();
        return IntradayTick.builder().timestamp(at).price(d(price)).volume(cumVolume).build();
    }

    private List<Candle> h1(List<IntradayTick> ticks) {
        return agg.fromTicks(ticks, CandleInterval.H1, OPEN, CLOSE, false);
    }

    @Test
    void sixTicksGiveTwoHourlyCandles() {
        List<Candle> c = h1(List.of(
                tick(DAY, 9, 5, "10.000", 10), tick(DAY, 9, 20, "10.500", 20), tick(DAY, 9, 50, "9.800", 30),
                tick(DAY, 10, 5, "9.900", 40), tick(DAY, 10, 20, "10.200", 50), tick(DAY, 10, 40, "10.100", 60)));
        assertEquals(2, c.size());
        assertEquals(d("10.000"), c.get(0).open());
        assertEquals(d("10.500"), c.get(0).high());
        assertEquals(d("9.800"), c.get(0).low());
        assertEquals(d("9.800"), c.get(0).close());
        assertEquals(d("9.900"), c.get(1).open());
        assertEquals(d("10.200"), c.get(1).high());
        assertEquals(d("9.900"), c.get(1).low());
        assertEquals(d("10.100"), c.get(1).close());
    }

    @Test
    void volumeIsDifferenceOfCumulativeVolume() {
        List<Candle> c = h1(List.of(
                tick(DAY, 9, 5, "10", 100), tick(DAY, 9, 30, "10", 250), tick(DAY, 9, 55, "10", 400),
                tick(DAY, 10, 10, "10", 600)));
        assertEquals(400, c.get(0).volume());
        assertEquals(200, c.get(1).volume());
    }

    @Test
    void gapInsideDayIsForwardFilledFlat() {
        List<Candle> c = h1(List.of(tick(DAY, 9, 10, "10.000", 100), tick(DAY, 11, 20, "11.000", 300)));
        assertEquals(3, c.size());
        Candle flat = c.get(1);
        assertEquals(d("10.000"), flat.open());
        assertEquals(d("10.000"), flat.high());
        assertEquals(d("10.000"), flat.low());
        assertEquals(d("10.000"), flat.close());
        assertEquals(0, flat.volume());
        assertEquals(200, c.get(2).volume());
    }

    @Test
    void fourHourBucketsAreNineAndThirteen() {
        List<Candle> c = agg.fromTicks(List.of(
                tick(DAY, 9, 5, "10", 1), tick(DAY, 12, 59, "10", 2), tick(DAY, 13, 5, "10", 3)),
                CandleInterval.H4, OPEN, CLOSE, false);
        assertEquals(2, c.size());
        assertEquals(ZonedDateTime.of(DAY, LocalTime.of(9, 0), MarketCalendar.ZONE).toEpochSecond(), c.get(0).time());
        assertEquals(ZonedDateTime.of(DAY, LocalTime.of(13, 0), MarketCalendar.ZONE).toEpochSecond(), c.get(1).time());
    }

    @Test
    void twoDaysNeverShareACandleAndVolumeRestarts() {
        LocalDate next = DAY.plusDays(3);   // Monday
        List<Candle> c = h1(List.of(
                tick(DAY, 13, 30, "10", 500), tick(next, 9, 10, "11", 40)));
        assertEquals(2, c.size());
        assertEquals(500, c.get(0).volume());
        assertEquals(40, c.get(1).volume());
        assertTrue(c.get(1).time() - c.get(0).time() > 24 * 3600);
    }

    @Test
    void ticksOutsideSessionAreIgnored() {
        List<Candle> c = h1(List.of(
                tick(DAY, 8, 59, "9", 1), tick(DAY, 9, 30, "10", 5), tick(DAY, 14, 30, "12", 9)));
        assertEquals(1, c.size());
        assertEquals(d("10.000"), c.get(0).close());
    }

    @Test
    void ignoreMarketHoursAlignsToMidnight() {
        List<Candle> c = agg.fromTicks(List.of(tick(DAY, 2, 30, "10", 1), tick(DAY, 17, 10, "11", 5)),
                CandleInterval.H1, OPEN, CLOSE, true);
        assertEquals(ZonedDateTime.of(DAY, LocalTime.of(2, 0), MarketCalendar.ZONE).toEpochSecond(), c.get(0).time());
        assertEquals(16, c.size());   // 02:00 .. 17:00
    }

    @Test
    void candleTimeIsEpochSecondsOfBucketStart() {
        List<Candle> c = h1(List.of(tick(DAY, 9, 5, "10", 1), tick(DAY, 10, 5, "10", 2)));
        long expected = Instant.parse("2026-11-06T09:00:00Z").getEpochSecond();   // 10:00 Tunis = 09:00Z
        assertEquals(expected, c.get(1).time());
    }

    @Test
    void weeklyGroupsMondayToFriday() {
        List<Candle> daily = new ArrayList<>();
        LocalDate monday = LocalDate.of(2026, 11, 2);
        for (int i = 0; i < 5; i++) {
            long time = monday.plusDays(i).atStartOfDay(MarketCalendar.ZONE).toEpochSecond();
            daily.add(new Candle(time, d("10").add(BigDecimal.valueOf(i)), d("12").add(BigDecimal.valueOf(i)),
                    d("9").add(BigDecimal.valueOf(i)), d("11").add(BigDecimal.valueOf(i)), 100));
        }
        List<Candle> weekly = agg.toWeekly(daily);
        assertEquals(1, weekly.size());
        Candle w = weekly.get(0);
        assertEquals(daily.get(0).time(), w.time());
        assertEquals(d("10"), w.open());
        assertEquals(d("16"), w.high());
        assertEquals(d("9"), w.low());
        assertEquals(d("15"), w.close());
        assertEquals(500, w.volume());
    }

    @Test
    void closingRefreshTickJoinsLastCandleOfTheDay() {
        // poll at 14:05, then the end-of-session refresh at 14:15 brings the official close
        List<Candle> c = agg.fromTicks(List.of(
                tick(DAY, 14, 0, "10.000", 100), tick(DAY, 14, 5, "10.100", 150), tick(DAY, 14, 15, "10.300", 400)),
                CandleInterval.M5, OPEN, CLOSE, false);
        assertEquals(2, c.size(), "14:15 tick must not open a candle after the close");
        Candle last = c.get(1);
        assertEquals(d("10.300"), last.close());
        assertEquals(300, last.volume());
        assertEquals(ZonedDateTime.of(DAY, LocalTime.of(14, 5), MarketCalendar.ZONE).toEpochSecond(), last.time());
    }
}
