package tn.esprit.pi5eme.repository.marketdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.IntradayTick;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IntradayTickRepository extends JpaRepository<IntradayTick, Long> {

    List<IntradayTick> findByInstrument_TickerIgnoreCaseAndTimestampBetweenOrderByTimestampAsc(String ticker, Instant from, Instant to);

    Optional<IntradayTick> findTopByInstrumentOrderByTimestampDesc(Instrument instrument);

    @Modifying
    @Query("delete from IntradayTick t where t.timestamp < :before")
    int deleteOlderThan(@Param("before") Instant before);

    /** Used to find the first tick of the day (= open). */
    @Query("select t from IntradayTick t where t.instrument = :instrument and t.timestamp >= :from order by t.timestamp asc")
    List<IntradayTick> findFromForInstrument(@Param("instrument") Instrument instrument, @Param("from") Instant from);

    /** Ticks of one instrument in [from, to), oldest first. */
    @Query("select t from IntradayTick t where t.instrument = :i and t.timestamp >= :from and t.timestamp < :to order by t.timestamp asc")
    List<IntradayTick> findRange(@Param("i") Instrument i, @Param("from") Instant from, @Param("to") Instant to);
}
