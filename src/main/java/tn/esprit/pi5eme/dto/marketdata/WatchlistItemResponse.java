package tn.esprit.pi5eme.dto.marketdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi5eme.entity.marketdata.WatchlistItem;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistItemResponse {

    private String ticker;
    private String name;
    private String sector;
    private LocalDateTime addedAt;
    /** Null if the instrument has no quote yet. */
    private QuoteResponse quote;

    public static WatchlistItemResponse fromEntity(WatchlistItem w, QuoteResponse quote) {
        return WatchlistItemResponse.builder()
                .ticker(w.getInstrument().getTicker())
                .name(w.getInstrument().getName())
                .sector(w.getInstrument().getSector())
                .addedAt(w.getAddedAt())
                .quote(quote)
                .build();
    }
}
