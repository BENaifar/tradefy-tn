package tn.esprit.pi5eme.dto.marketdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi5eme.entity.marketdata.IntradayTick;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntradayTickResponse {

    private Instant timestamp;
    private BigDecimal price;
    private long volume;

    public static IntradayTickResponse fromEntity(IntradayTick t) {
        return IntradayTickResponse.builder()
                .timestamp(t.getTimestamp()).price(t.getPrice()).volume(t.getVolume())
                .build();
    }
}
