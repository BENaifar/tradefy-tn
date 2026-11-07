package tn.esprit.pi5eme.entity.portfolio;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Plain id (not a relation to User) to keep modules decoupled. */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String name = "My Portfolio";

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String baseCurrency = "TND";

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal initialCapital;

    /** Total cash, including reserved cash. */
    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal cashBalance;

    @Column(nullable = false, precision = 19, scale = 3)
    @Builder.Default
    private BigDecimal reservedCash = BigDecimal.ZERO.setScale(3);

    /** initialCapital + deposits - withdrawals. */
    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal netContributions;

    @Column(nullable = false, precision = 19, scale = 3)
    @Builder.Default
    private BigDecimal realizedPnl = BigDecimal.ZERO.setScale(3);

    @Column(nullable = false, precision = 19, scale = 3)
    @Builder.Default
    private BigDecimal totalFees = BigDecimal.ZERO.setScale(3);

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public BigDecimal getAvailableCash() {
        return cashBalance.subtract(reservedCash);
    }
}
