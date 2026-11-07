package tn.esprit.pi5eme.repository.marketdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.pi5eme.entity.marketdata.Instrument;

import java.util.List;
import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    Optional<Instrument> findByTickerIgnoreCase(String ticker);

    Optional<Instrument> findByIsin(String isin);

    List<Instrument> findByActiveTrueOrderByTickerAsc();

    @Query("select i from Instrument i where i.active = true "
            + "and (cast(:q as string) is null or lower(i.ticker) like lower(concat('%', :q, '%')) or lower(i.name) like lower(concat('%', :q, '%'))) "
            + "and (cast(:sector as string) is null or i.sector = :sector) order by i.ticker")
    List<Instrument> search(@Param("q") String q, @Param("sector") String sector);

    @Query("select distinct i.sector from Instrument i where i.sector is not null order by i.sector")
    List<String> findDistinctSectors();
}
