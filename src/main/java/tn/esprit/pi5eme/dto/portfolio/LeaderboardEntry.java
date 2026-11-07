package tn.esprit.pi5eme.dto.portfolio;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Never exposes emails. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {

    private int rank;
    /** For example "John D.". */
    private String displayName;
    private BigDecimal totalReturnPct;
    private BigDecimal totalValue;
    @JsonProperty("isMe")
    private boolean me;
}
