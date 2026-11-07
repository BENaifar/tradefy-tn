package tn.esprit.pi5eme.repository.marketdata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.PriceBar;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PriceBarRepositoryTest {

    @Autowired
    InstrumentRepository instruments;
    @Autowired
    PriceBarRepository bars;

    @Test
    void duplicateInstrumentAndDateRejected() {
        Instrument i = instruments.save(Instrument.builder().ticker("AB").isin("TN0000000001").name("AMEN").build());
        LocalDate d = LocalDate.of(2026, 11, 6);
        bars.saveAndFlush(PriceBar.builder().instrument(i).date(d).close(BigDecimal.ONE).build());
        assertThrows(DataIntegrityViolationException.class,
                () -> bars.saveAndFlush(PriceBar.builder().instrument(i).date(d).close(BigDecimal.TEN).build()));
    }
}
