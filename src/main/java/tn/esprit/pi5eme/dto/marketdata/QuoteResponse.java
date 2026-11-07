package tn.esprit.pi5eme.dto.marketdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi5eme.entity.marketdata.Quote;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteResponse {

    private String ticker;
    private String name;
    private BigDecimal last;
    private BigDecimal previousClose;
    /** Absolute change: last - previousClose. */
    private BigDecimal change;
    private BigDecimal changePercent;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private long volume;
    private BigDecimal turnover;
    private BigDecimal bid;
    private BigDecimal ask;
    private long bidQty;
    private long askQty;
    private BigDecimal upperLimit;
    private BigDecimal lowerLimit;
    private LocalDate sessionDate;
    private LocalTime quoteTime;
    private Instant updatedAt;

    public static QuoteResponse fromEntity(Quote q) {
        BigDecimal change = (q.getLast() != null && q.getPreviousClose() != null)
                ? MoneyUtils.money(q.getLast().subtract(q.getPreviousClose()))
                : null;
        return QuoteResponse.builder()
                .ticker(q.getInstrument().getTicker())
                .name(q.getInstrument().getName())
                .last(q.getLast())
                .previousClose(q.getPreviousClose())
                .change(change)
                .changePercent(q.getChangePercent())
                .open(q.getOpen())
                .high(q.getHigh())
                .low(q.getLow())
                .volume(q.getVolume())
                .turnover(q.getTurnover())
                .bid(q.getBid())
                .ask(q.getAsk())
                .bidQty(q.getBidQty())
                .askQty(q.getAskQty())
                .upperLimit(q.getUpperLimit())
                .lowerLimit(q.getLowerLimit())
                .sessionDate(q.getSessionDate())
                .quoteTime(q.getQuoteTime())
                .updatedAt(q.getUpdatedAt())
                .build();
    }
}
