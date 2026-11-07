package tn.esprit.pi5eme.dto.portfolio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi5eme.entity.portfolio.TargetType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationTargetRequest {

    @NotNull
    private TargetType type;

    /** A ticker or a sector name. */
    @NotBlank
    private String key;

    /** In (0, 100]. */
    @NotNull
    private BigDecimal percent;
}
