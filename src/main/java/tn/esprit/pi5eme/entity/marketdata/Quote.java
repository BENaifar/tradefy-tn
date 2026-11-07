package tn.esprit.pi5eme.entity.marketdata;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** One row per instrument, overwritten on every refresh. */
@Entity
@Table(name = "quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", unique = true, nullable = false)
    private Instrument instrument;

    @Column(name = "last_price", precision = 19, scale = 3)
    private BigDecimal last;

    @Column(precision = 19, scale = 3)
    private BigDecimal previousClose;

    @Column(name = "open_price", precision = 19, scale = 3)
    private BigDecimal open;

    @Column(name = "high_price", precision = 19, scale = 3)
    private BigDecimal high;

    @Column(name = "low_price", precision = 19, scale = 3)
    private BigDecimal low;

    @Column(precision = 9, scale = 4)
    private BigDecimal changePercent;

    private long volume;

    @Column(precision = 19, scale = 3)
    private BigDecimal turnover;

    @Column(precision = 19, scale = 3)
    private BigDecimal bid;

    @Column(precision = 19, scale = 3)
    private BigDecimal ask;

    private long bidQty;
    private long askQty;

    @Column(precision = 19, scale = 3)
    private BigDecimal upperLimit;

    @Column(precision = 19, scale = 3)
    private BigDecimal lowerLimit;

    private LocalDate sessionDate;
    private LocalTime quoteTime;
    private Instant updatedAt;
}
