package tn.esprit.pi5eme.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi5eme.service.portfolio.PerformanceRange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceResponse {

    /** Ticker with its unrealized return among open positions. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionPerformance {
        private String ticker;
        private BigDecimal unrealizedPnlPct;
    }

    private PerformanceRange range;
    private LocalDate from;
    private LocalDate to;
    private List<EquityPoint> points;
    private BigDecimal startValue;
    private BigDecimal endValue;
    /** Compounded daily returns over the range. */
    private BigDecimal periodReturnPct;
    private BigDecimal totalReturnPct;
    private BigDecimal realizedPnl;
    private BigDecimal unrealizedPnl;
    private PositionPerformance bestPosition;
    private PositionPerformance worstPosition;
    private long winningTrades;
    private long losingTrades;
    private BigDecimal winRatePct;
}
