package tn.esprit.pi5eme.entity.portfolio;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/** Ledger row. Rows are only ever appended. */
@Entity
@Table(name = "portfolio_transactions",
        indexes = @Index(name = "idx_ptx_portfolio_time", columnList = "portfolio_id, executed_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(length = 20)
    private String ticker;

    private Long quantity;

    @Column(precision = 19, scale = 3)
    private BigDecimal price;

    /** qty x price for trades, the amount for cash operations. */
    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 19, scale = 3)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO.setScale(3);

    /** Signed effect on cash. */
    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal netAmount;

    @Column(precision = 19, scale = 3)
    private BigDecimal realizedPnl;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal cashBalanceAfter;

    private String orderId;

    private String executionId;

    @Column(length = 255)
    private String note;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;
}
