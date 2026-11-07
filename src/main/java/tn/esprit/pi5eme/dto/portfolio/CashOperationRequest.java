package tn.esprit.pi5eme.dto.portfolio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashOperationRequest {

    @NotNull
    @DecimalMin(value = "0.001")
    @Digits(integer = 16, fraction = 3)
    private BigDecimal amount;

    @Size(max = 255)
    private String note;
}
