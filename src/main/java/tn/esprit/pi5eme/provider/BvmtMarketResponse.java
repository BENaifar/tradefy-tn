package tn.esprit.pi5eme.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** Jackson mirror of the BVMT market JSON. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BvmtMarketResponse {

    private List<Item> markets;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Item {
        private String isin;
        private BigDecimal last, close, change, open, high, low, caps, max, min;
        private Long volume;
        private String seance, time;
        private Limit limit;
        private Referentiel referentiel;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Limit {
        private BigDecimal bid, ask;
        private Long bidQty, askQty;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Referentiel {
        private String ticker, stockName, arabName, isin, valGroup;
    }
}
