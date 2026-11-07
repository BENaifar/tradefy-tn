package tn.esprit.pi5eme.repository.portfolio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PortfolioRepositoryTest {

    @Autowired
    PortfolioRepository repo;

    private Portfolio portfolio(long userId) {
        BigDecimal c = new BigDecimal("100000.000");
        return Portfolio.builder().userId(userId).initialCapital(c).cashBalance(c).netContributions(c).build();
    }

    @Test
    void findByUserId() {
        repo.saveAndFlush(portfolio(7L));
        assertEquals(7L, repo.findByUserId(7L).orElseThrow().getUserId());
    }

    @Test
    void secondPortfolioForSameUserRejected() {
        repo.saveAndFlush(portfolio(7L));
        assertThrows(DataIntegrityViolationException.class, () -> repo.saveAndFlush(portfolio(7L)));
    }
}
