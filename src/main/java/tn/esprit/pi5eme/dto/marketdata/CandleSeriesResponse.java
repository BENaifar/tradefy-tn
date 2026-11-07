package tn.esprit.pi5eme.dto.marketdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandleSeriesResponse {

    private String ticker;
    private CandleInterval interval;
    private LocalDate from;
    private LocalDate to;
    /** Always Africa/Tunis. */
    private String timezone;
    /** True when the last candle is today's in-progress candle built from the live quote. */
    private boolean live;
    /** True when more than 5000 candles matched and only the most recent ones are returned. */
    private boolean truncated;
    private List<Candle> candles;
}
