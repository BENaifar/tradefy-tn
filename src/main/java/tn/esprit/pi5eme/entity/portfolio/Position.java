package tn.esprit.pi5eme.entity.portfolio;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false, length = 20)
    private String ticker;

    private long quantity;

    /** Shares locked by pending SELL orders. */
    private long reservedQuantity;

    /** Includes buy fees. */
    @Column(nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal avgCost = BigDecimal.ZERO.setScale(6);

    @Column(nullable = false, precision = 19, scale = 3)
    @Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO.setScale(3);

    @Column(nullable = false, precision = 19, scale = 3)
    @Builder.Default
    private BigDecimal realizedPnl = BigDecimal.ZERO.setScale(3);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private PositionStatus status = PositionStatus.OPEN;

    private Instant openedAt;
    private Instant closedAt;

    public long getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
}
