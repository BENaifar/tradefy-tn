package tn.esprit.pi5eme.repository.marketdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.WatchlistItem;

import java.util.List;
import java.util.Optional;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    @Query("select w from WatchlistItem w join fetch w.instrument where w.userId = :userId order by w.addedAt asc, w.id asc")
    List<WatchlistItem> findByUserIdOrderByAddedAtAsc(@Param("userId") Long userId);

    Optional<WatchlistItem> findByUserIdAndInstrument_TickerIgnoreCase(Long userId, String ticker);

    boolean existsByUserIdAndInstrument(Long userId, Instrument instrument);
}
