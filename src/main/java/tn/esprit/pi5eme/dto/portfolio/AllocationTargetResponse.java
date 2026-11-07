package tn.esprit.pi5eme.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi5eme.entity.portfolio.AllocationTarget;
import tn.esprit.pi5eme.entity.portfolio.TargetType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationTargetResponse {

    private TargetType type;
    private String key;
    private BigDecimal percent;

    public static AllocationTargetResponse fromEntity(AllocationTarget t) {
        return AllocationTargetResponse.builder()
                .type(t.getType()).key(t.getTargetKey()).percent(t.getTargetPercent()).build();
    }
}
