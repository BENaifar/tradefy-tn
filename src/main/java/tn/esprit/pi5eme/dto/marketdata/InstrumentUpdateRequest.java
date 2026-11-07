package tn.esprit.pi5eme.dto.marketdata;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Null fields mean "leave unchanged". */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentUpdateRequest {

    @Size(max = 60)
    private String sector;

    private Boolean active;
}
