package tn.esprit.pi5eme.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.marketdata")
@Getter
@Setter
public class MarketDataProperties {
    private String provider = "bvmt";                // "bvmt" | "simulated"
    private String bvmtUrl = "https://www.bvmt.com.tn/rest_api/rest/market/groups/11,12,52,95,99";
    private Duration pollInterval = Duration.ofSeconds(30);
    private Duration httpTimeout = Duration.ofSeconds(10);
    private boolean syncOnStartup = true;
    private boolean ignoreMarketHours = false;       // true = poll 24/7
    private LocalTime sessionOpen = LocalTime.of(9, 0);
    private LocalTime sessionClose = LocalTime.of(14, 10);
    private List<LocalDate> holidays = new ArrayList<>();
    private int intradayRetentionDays = 180;
    private BigDecimal simulatedVolatility = new BigDecimal("0.004"); // 0.4% per tick
    private Long simulatedSeed;                                         // optional, for reproducible tests
}
