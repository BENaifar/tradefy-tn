package tn.esprit.pi5eme.service.marketdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.CandleInterval;
import tn.esprit.pi5eme.dto.marketdata.CandleSeriesResponse;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.IntradayTick;
import tn.esprit.pi5eme.entity.marketdata.PriceBar;
import tn.esprit.pi5eme.entity.marketdata.Quote;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.exception.ResourceNotFoundException;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.repository.marketdata.*;
import tn.esprit.pi5eme.scheduler.MarketCalendar;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Fixed clock: Friday 2026-11-06, 11:00 in Tunis. */
@SpringBootTest
@Transactional
class CandleServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(ZonedDateTime.of(TODAY, LocalTime.of(11, 0), MarketCalendar.ZONE).toInstant(),
                    MarketCalendar.ZONE);
        }
    }

    @MockBean MarketDataProvider provider;
    @Autowired ICandleService service;
    @Autowired InstrumentRepository instruments;
    @Autowired IntradayTickRepository ticks;
    @Autowired PriceBarRepository bars;
    @Autowired QuoteRepository quotes;

    private Instrument instrument;

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    @BeforeEach
    void setUp() {
        instrument = instruments.save(Instrument.builder().ticker("CAN").isin("TNCAN0000001").name("CANDLE").build());
    }

    private void tick(LocalDate day, int hour, int minute, String price, long cum) {
        ticks.save(IntradayTick.builder().instrument(instrument)
                .timestamp(ZonedDateTime.of(day, LocalTime.of(hour, minute), MarketCalendar.ZONE).toInstant())
                .price(d(price)).volume(cum).build());
    }

    private void bar(LocalDate day, String close) {
        bars.save(PriceBar.builder().instrument(instrument).date(day).open(d(close)).high(d(close)).low(d(close))
                .close(d(close)).volume(10).build());
    }

    private void todayQuote() {
        quotes.save(Quote.builder().instrument(instrument).last(d("12.000")).previousClose(d("11.000"))
                .open(d("11.500")).high(d("12.500")).low(d("11.200")).volume(777).sessionDate(TODAY)
                .updatedAt(Instant.now()).build());
    }

    @Test
    void hourlyOverTwoDays() {
        LocalDate yesterday = TODAY.minusDays(1);
        tick(yesterday, 9, 10, "10", 10);
        tick(yesterday, 10, 10, "11", 20);
        tick(TODAY, 9, 20, "12", 5);
        List<Candle> c = service.getCandles("CAN", CandleInterval.H1, yesterday, TODAY);
        assertEquals(3, c.size());
    }

    @Test
    void dailyAddsLiveCandleFromQuote() {
        bar(TODAY.minusDays(3), "10");
        bar(TODAY.minusDays(2), "10.5");
        bar(TODAY.minusDays(1), "11");
        todayQuote();
        CandleSeriesResponse r = service.getSeries("CAN", CandleInterval.D1, TODAY.minusDays(10), TODAY);
        assertEquals(4, r.getCandles().size());
        assertTrue(r.isLive());
        Candle live = r.getCandles().get(3);
        assertEquals(d("11.500"), live.open());
        assertEquals(d("12.000"), live.close());
        assertEquals(777, live.volume());
        assertEquals("Africa/Tunis", r.getTimezone());
    }

    @Test
    void dailyDoesNotDuplicateWhenTodaysBarExists() {
        bar(TODAY.minusDays(1), "11");
        bar(TODAY, "12");
        todayQuote();
        CandleSeriesResponse r = service.getSeries("CAN", CandleInterval.D1, TODAY.minusDays(10), TODAY);
        assertEquals(2, r.getCandles().size());
        assertFalse(r.isLive());
    }

    @Test
    void weeklyGroupsDays() {
        bar(LocalDate.of(2026, 10, 26), "10");   // Monday of previous week
        bar(LocalDate.of(2026, 10, 27), "11");
        bar(LocalDate.of(2026, 11, 2), "12");    // Monday of this week
        bar(LocalDate.of(2026, 11, 3), "13");
        List<Candle> c = service.getCandles("CAN", CandleInterval.W1, LocalDate.of(2026, 10, 20), TODAY);
        assertEquals(2, c.size());
        assertEquals(d("11"), c.get(0).close());
        assertEquals(d("13"), c.get(1).close());
    }

    @Test
    void unknownTickerIs404() {
        assertThrows(ResourceNotFoundException.class, () -> service.getCandles("NOPE", CandleInterval.D1, null, null));
    }

    @Test
    void fromAfterToIs400() {
        assertThrows(BadRequestException.class,
                () -> service.getCandles("CAN", CandleInterval.D1, TODAY, TODAY.minusDays(2)));
    }

    @Test
    void lookbackReturnsExtraCandlesBeforeFrom() {
        for (int i = 1; i <= 60; i++) {
            bar(TODAY.minusDays(i + 20L), "10");
        }
        LocalDate from = TODAY.minusDays(10);
        List<Candle> c = service.getCandlesWithLookback("CAN", CandleInterval.D1, from, TODAY, 30);
        long cutoff = from.atStartOfDay(MarketCalendar.ZONE).toEpochSecond();
        assertTrue(c.stream().filter(x -> x.time() < cutoff).count() >= 30);
    }
}
