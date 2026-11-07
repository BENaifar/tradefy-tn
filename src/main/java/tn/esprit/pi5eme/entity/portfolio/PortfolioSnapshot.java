package tn.esprit.pi5eme.entity.portfolio;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "portfolio_snapshots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "snapshot_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate date;

    @Column(precision = 19, scale = 3)
    private BigDecimal cash;

    @Column(precision = 19, scale = 3)
    private BigDecimal marketValue;

    @Column(precision = 19, scale = 3)
    private BigDecimal totalValue;

    @Column(precision = 19, scale = 3)
    private BigDecimal netContributions;

    /** Deposits - withdrawals since the previous snapshot. */
    @Column(precision = 19, scale = 3)
    private BigDecimal netFlow;

    @Column(precision = 9, scale = 4)
    private BigDecimal dailyReturnPct;

    @Column(precision = 9, scale = 4)
    private BigDecimal cumulativeReturnPct;
}
