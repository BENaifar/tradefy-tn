package tn.esprit.pi5eme.scheduler;

import org.junit.jupiter.api.Test;
import tn.esprit.pi5eme.config.MarketDataProperties;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketCalendarTest {

    private static final ZoneId TUNIS = MarketCalendar.ZONE;

    private ZonedDateTime at(String iso) {
        return ZonedDateTime.parse(iso + "[Africa/Tunis]");
    }

    private MarketCalendar cal(MarketDataProperties p, ZonedDateTime now) {
        return new MarketCalendar(p, Clock.fixed(now.toInstant(), TUNIS));
    }

    @Test
    void openFridayMorning() {
        ZonedDateTime t = at("2026-11-06T10:00:00+01:00");
        assertTrue(cal(new MarketDataProperties(), t).isOpen(t));
    }

    @Test
    void closedAfterClose() {
        ZonedDateTime t = at("2026-11-06T14:30:00+01:00");
        assertFalse(cal(new MarketDataProperties(), t).isOpen(t));
    }

    @Test
    void closedOnSaturday() {
        ZonedDateTime t = at("2026-11-07T10:00:00+01:00");
        assertFalse(cal(new MarketDataProperties(), t).isOpen(t));
    }

    @Test
    void closedOnHoliday() {
        MarketDataProperties p = new MarketDataProperties();
        p.getHolidays().add(LocalDate.of(2026, 11, 6));
        ZonedDateTime t = at("2026-11-06T10:00:00+01:00");
        assertFalse(cal(p, t).isOpen(t));
    }

    @Test
    void ignoreMarketHoursAlwaysOpen() {
        MarketDataProperties p = new MarketDataProperties();
        p.setIgnoreMarketHours(true);
        assertTrue(cal(p, at("2026-11-07T03:00:00+01:00")).isOpenNow());
    }

    @Test
    void todayUsesTunisZone() {
        ZonedDateTime t = at("2026-11-06T23:30:00+01:00");
        assertEquals(LocalDate.of(2026, 11, 6), cal(new MarketDataProperties(), t).today());
    }
}
