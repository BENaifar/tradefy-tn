package tn.esprit.pi5eme.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi5eme.service.portfolio.AllocationGroupBy;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationResponse {

    private AllocationGroupBy groupBy;
    private BigDecimal totalValue;
    private List<AllocationSlice> slices;
}
