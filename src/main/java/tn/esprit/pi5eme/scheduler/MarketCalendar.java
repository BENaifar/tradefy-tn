package tn.esprit.pi5eme.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.config.MarketDataProperties;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
public class MarketCalendar {

    public static final ZoneId ZONE = ZoneId.of("Africa/Tunis");

    private final MarketDataProperties props;
    private final Clock clock;

    public boolean isTradingDay(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !props.getHolidays().contains(d);
    }

    public boolean isOpen(ZonedDateTime now) {
        ZonedDateTime local = now.withZoneSameInstant(ZONE);
        if (!isTradingDay(local.toLocalDate())) {
            return false;
        }
        LocalTime t = local.toLocalTime();
        return !t.isBefore(props.getSessionOpen()) && t.isBefore(props.getSessionClose());
    }

    public boolean isOpenNow() {
        return props.isIgnoreMarketHours() || isOpen(ZonedDateTime.now(clock));
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(ZONE));
    }
}
