package tn.esprit.pi5eme.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi5eme.entity.portfolio.PositionStatus;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionResponse {

    private Long id;
    private String ticker;
    private String name;
    private String sector;
    private PositionStatus status;
    private long quantity;
    private long reservedQuantity;
    private long availableQuantity;
    private BigDecimal avgCost;
    private BigDecimal totalCost;
    private BigDecimal lastPrice;
    private BigDecimal marketValue;
    private BigDecimal unrealizedPnl;
    private BigDecimal unrealizedPnlPct;
    private BigDecimal dayChangePct;
    private BigDecimal realizedPnl;
    private BigDecimal weightPct;
    private Instant openedAt;
    private Instant closedAt;
    private long holdingDays;
}
