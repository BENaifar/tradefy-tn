package tn.esprit.pi5eme.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryResponse {

    private Long portfolioId;
    private String name;
    private String currency;
    private BigDecimal initialCapital;
    private BigDecimal netContributions;
    private BigDecimal cashBalance;
    private BigDecimal reservedCash;
    private BigDecimal availableCash;
    private BigDecimal marketValue;
    private BigDecimal totalValue;
    private BigDecimal realizedPnl;
    private BigDecimal unrealizedPnl;
    /** totalValue - netContributions. */
    private BigDecimal totalPnl;
    private BigDecimal totalReturnPct;
    private BigDecimal dayPnl;
    private BigDecimal dayPnlPct;
    private BigDecimal totalFees;
    private int openPositionsCount;
    private List<String> missingPrices;
    private LocalDateTime createdAt;
}
