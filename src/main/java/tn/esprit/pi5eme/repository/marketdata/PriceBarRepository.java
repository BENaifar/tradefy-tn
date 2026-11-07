package tn.esprit.pi5eme.repository.marketdata;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.PriceBar;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceBarRepository extends JpaRepository<PriceBar, Long> {

    Optional<PriceBar> findByInstrumentAndDate(Instrument instrument, LocalDate date);

    List<PriceBar> findByInstrument_TickerIgnoreCaseAndDateBetweenOrderByDateAsc(String ticker, LocalDate from, LocalDate to);
}
