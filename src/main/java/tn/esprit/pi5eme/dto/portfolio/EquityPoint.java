package tn.esprit.pi5eme.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquityPoint {

    private LocalDate date;
    private BigDecimal totalValue;
    private BigDecimal cumulativeReturnPct;
}
