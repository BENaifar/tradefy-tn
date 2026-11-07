package tn.esprit.pi5eme.entity.marketdata;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "intraday_ticks",
        indexes = @Index(name = "idx_tick_instrument_time", columnList = "instrument_id, tick_time"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntradayTick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "tick_time", nullable = false)
    private Instant timestamp;

    @Column(precision = 19, scale = 3)
    private BigDecimal price;

    /** Cumulative day volume at that moment. */
    private long volume;
}
