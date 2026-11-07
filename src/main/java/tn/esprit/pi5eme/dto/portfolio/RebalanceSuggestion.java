package tn.esprit.pi5eme.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi5eme.entity.portfolio.OrderSide;

import java.math.BigDecimal;

/** A suggestion only; nothing is executed. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebalanceSuggestion {

    private String ticker;
    private OrderSide side;
    private long quantity;
    private BigDecimal estimatedPrice;
    private BigDecimal estimatedAmount;
    private BigDecimal currentWeightPct;
    private BigDecimal targetWeightPct;
}
