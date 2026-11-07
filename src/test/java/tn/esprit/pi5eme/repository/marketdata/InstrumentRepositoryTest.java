package tn.esprit.pi5eme.repository.marketdata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import tn.esprit.pi5eme.entity.marketdata.Instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InstrumentRepositoryTest {

    @Autowired
    InstrumentRepository repo;

    private Instrument inst(String t, String isin, String name, String sector) {
        return Instrument.builder().ticker(t).isin(isin).name(name).sector(sector).build();
    }

    @Test
    void searchByNameAndSector() {
        repo.save(inst("AB", "TN0000000001", "AMEN BANK", "Banques"));
        repo.save(inst("ATB", "TN0000000002", "ARAB TUNISIAN BANK", "Banques"));
        repo.save(inst("AL", "TN0000000003", "AIR LIQUIDE", "Industrie"));

        assertEquals(2, repo.search("bank", null).size());
        assertEquals(2, repo.search(null, "Banques").size());
        assertEquals(3, repo.search(null, null).size());
    }
}
