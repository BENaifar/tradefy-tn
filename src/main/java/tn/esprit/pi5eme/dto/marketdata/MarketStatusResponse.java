package tn.esprit.pi5eme.dto.marketdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketStatusResponse {

    private boolean open;
    private String provider;
    private Instant lastSyncAt;
    private LocalTime sessionOpen;
    private LocalTime sessionClose;
    private String zone;
    private long instrumentsCount;
}
