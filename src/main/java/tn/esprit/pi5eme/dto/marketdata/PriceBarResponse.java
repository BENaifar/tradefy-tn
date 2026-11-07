package tn.esprit.pi5eme.dto.marketdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi5eme.entity.marketdata.PriceBar;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceBarResponse {

    private LocalDate date;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private long volume;

    public static PriceBarResponse fromEntity(PriceBar b) {
        return PriceBarResponse.builder()
                .date(b.getDate()).open(b.getOpen()).high(b.getHigh())
                .low(b.getLow()).close(b.getClose()).volume(b.getVolume())
                .build();
    }
}
