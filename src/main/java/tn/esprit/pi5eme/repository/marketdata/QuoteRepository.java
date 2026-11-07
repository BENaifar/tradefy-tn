package tn.esprit.pi5eme.repository.marketdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.Quote;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    @Query("select q from Quote q join fetch q.instrument i where upper(i.ticker) = upper(:ticker)")
    Optional<Quote> findByTicker(@Param("ticker") String ticker);

    Optional<Quote> findByInstrument(Instrument instrument);

    @Query("select q from Quote q join fetch q.instrument i where i.active = true")
    List<Quote> findAllActiveWithInstrument();

    @Query("select q from Quote q join fetch q.instrument i where upper(i.ticker) in :tickers")
    List<Quote> findByTickers(@Param("tickers") Collection<String> upperCaseTickers);
}
