package tn.esprit.pi5eme.dto.portfolio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi5eme.entity.portfolio.OrderSide;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevExecutionRequest {

    @NotBlank
    private String ticker;

    @NotNull
    private OrderSide side;

    @Positive
    private long quantity;

    /** Null means the last market price. */
    private BigDecimal price;
}
