package tn.esprit.pi5eme.entity.portfolio;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "allocation_targets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "type", "target_key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 12)
    private TargetType type;

    /** A ticker or a sector name. */
    @Column(name = "target_key", nullable = false, length = 60)
    private String targetKey;

    @Column(nullable = false, precision = 9, scale = 4)
    private BigDecimal targetPercent;
}
