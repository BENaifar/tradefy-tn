package tn.esprit.pi5eme.service.marketdata.indicator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.pi5eme.config.MarketDataProperties;
import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.CandleInterval;
import tn.esprit.pi5eme.dto.marketdata.indicator.IndicatorPoint;
import tn.esprit.pi5eme.dto.marketdata.indicator.IndicatorResult;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.exception.ResourceNotFoundException;
import tn.esprit.pi5eme.scheduler.MarketCalendar;
import tn.esprit.pi5eme.service.marketdata.ICandleService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IndicatorServiceImplTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 2);
    private static final LocalDate TO = LocalDate.of(2026, 11, 6);

    private ICandleService candleService;
    private IndicatorServiceImpl service;

    @BeforeEach
    void setUp() {
        candleService = mock(ICandleService.class);
        service = new IndicatorServiceImpl(new IndicatorRegistry(List.of(new SmaIndicator(), new MacdIndicator())),
                candleService, new MarketCalendar(new MarketDataProperties(), Clock.systemUTC()));
        // 6 warm-up candles before FROM, then FROM..TO (5 days), closes 1..11
        List<Candle> candles = new ArrayList<>();
        long start = FROM.minusDays(6).atStartOfDay(MarketCalendar.ZONE).toEpochSecond();
        for (int i = 0; i < 11; i++) {
            BigDecimal c = BigDecimal.valueOf(i + 1);
            candles.add(new Candle(start + i * 86_400L, c, c, c, c, 1));
        }
        when(candleService.getCandlesWithLookback(eq("TJARI"), eq(CandleInterval.D1), any(), any(), anyInt()))
                .thenReturn(candles);
    }

    @Test
    void defaultsAreApplied() {
        IndicatorResult r = service.compute("TJARI", "SMA", CandleInterval.D1, FROM, TO, Map.of());
        assertEquals(20.0, r.params().get("period"));
        assertEquals("SMA", r.code());
    }

    @Test
    void warmupCandlesAreTrimmedSoOutputStartsAtFrom() {
        IndicatorResult r = service.compute("TJARI", "sma", CandleInterval.D1, FROM, TO, Map.of("period", "3"));
        List<IndicatorPoint> points = r.series().get("value");
        long cutoff = FROM.atStartOfDay(MarketCalendar.ZONE).toEpochSecond();
        assertEquals(5, points.size());
        assertEquals(cutoff, points.get(0).time());
        assertEquals(0, new BigDecimal("6").compareTo(points.get(0).value()));   // mean of 5,6,7
    }

    @Test
    void notComputableValuesBecomeNull() {
        IndicatorResult r = service.compute("TJARI", "SMA", CandleInterval.D1, FROM, TO, Map.of("period", "500"));
        assertTrue(r.series().get("value").stream().allMatch(p -> p.value() == null));
    }

    @Test
    void nonNumericParamIs400() {
        assertThrows(BadRequestException.class,
                () -> service.compute("TJARI", "SMA", CandleInterval.D1, FROM, TO, Map.of("period", "abc")));
    }

    @Test
    void belowMinimumIs400() {
        assertThrows(BadRequestException.class,
                () -> service.compute("TJARI", "SMA", CandleInterval.D1, FROM, TO, Map.of("period", "0")));
    }

    @Test
    void fractionalIntParamIs400() {
        assertThrows(BadRequestException.class,
                () -> service.compute("TJARI", "SMA", CandleInterval.D1, FROM, TO, Map.of("period", "2.5")));
    }

    @Test
    void typoedParamNameIs400() {
        assertThrows(BadRequestException.class,
                () -> service.compute("TJARI", "SMA", CandleInterval.D1, FROM, TO, Map.of("perid", "5")));
    }

    @Test
    void unknownCodeIs404() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.compute("TJARI", "NOPE", CandleInterval.D1, FROM, TO, Map.of()));
    }

    @Test
    void crossParameterValidationRuns() {
        assertThrows(BadRequestException.class, () -> service.compute("TJARI", "MACD", CandleInterval.D1, FROM, TO,
                Map.of("fast", "26", "slow", "12")));
    }
}
