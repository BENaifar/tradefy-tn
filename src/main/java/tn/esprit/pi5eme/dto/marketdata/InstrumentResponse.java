package tn.esprit.pi5eme.dto.marketdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi5eme.entity.marketdata.AssetClass;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.Market;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentResponse {

    private Long id;
    private String ticker;
    private String isin;
    private String name;
    private String arabName;
    private AssetClass assetClass;
    private Market market;
    private String valGroup;
    private String sector;
    private String currency;
    private boolean active;
    private QuoteResponse quote;

    public static InstrumentResponse fromEntity(Instrument i, QuoteResponse quote) {
        return InstrumentResponse.builder()
                .id(i.getId())
                .ticker(i.getTicker())
                .isin(i.getIsin())
                .name(i.getName())
                .arabName(i.getArabName())
                .assetClass(i.getAssetClass())
                .market(i.getMarket())
                .valGroup(i.getValGroup())
                .sector(i.getSector())
                .currency(i.getCurrency())
                .active(i.isActive())
                .quote(quote)
                .build();
    }

    public static InstrumentResponse fromEntity(Instrument i) {
        return fromEntity(i, null);
    }
}
