package tn.esprit.pi5eme.entity.marketdata;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "instruments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String ticker;

    @Column(nullable = false, unique = true, length = 12)
    private String isin;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String arabName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private AssetClass assetClass = AssetClass.EQUITY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Market market = Market.BVMT;

    @Column(length = 5)
    private String valGroup;

    @Column(length = 60)
    private String sector;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "TND";

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
