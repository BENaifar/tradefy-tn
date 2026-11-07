package tn.esprit.pi5eme.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationSlice {

    private String key;
    private String label;
    private BigDecimal value;
    private BigDecimal weightPct;
    /** Null when no target is set for this slice. */
    private BigDecimal targetPct;
    /** weight - target; null when no target. */
    private BigDecimal driftPct;
}
