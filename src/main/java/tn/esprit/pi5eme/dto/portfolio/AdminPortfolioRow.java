package tn.esprit.pi5eme.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPortfolioRow {

    private Long userId;
    private String email;
    private String fullName;
    private BigDecimal totalValue;
    private BigDecimal totalReturnPct;
    private int openPositions;
    private Instant lastActivity;
}
