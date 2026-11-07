package tn.esprit.pi5eme.entity.marketdata;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "price_bars",
        uniqueConstraints = @UniqueConstraint(columnNames = {"instrument_id", "bar_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceBar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "bar_date", nullable = false)
    private LocalDate date;

    @Column(name = "open_price", precision = 19, scale = 3)
    private BigDecimal open;

    @Column(name = "high_price", precision = 19, scale = 3)
    private BigDecimal high;

    @Column(name = "low_price", precision = 19, scale = 3)
    private BigDecimal low;

    @Column(name = "close_price", precision = 19, scale = 3)
    private BigDecimal close;

    private long volume;
}
