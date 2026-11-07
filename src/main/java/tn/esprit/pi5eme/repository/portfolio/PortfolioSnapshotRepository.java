package tn.esprit.pi5eme.repository.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;
import tn.esprit.pi5eme.entity.portfolio.PortfolioSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    Optional<PortfolioSnapshot> findByPortfolioAndDate(Portfolio p, LocalDate d);

    Optional<PortfolioSnapshot> findTopByPortfolioAndDateLessThanOrderByDateDesc(Portfolio p, LocalDate d);

    List<PortfolioSnapshot> findByPortfolioAndDateBetweenOrderByDateAsc(Portfolio p, LocalDate from, LocalDate to);

    /** Used by reset: snapshots from the reset day on describe the old portfolio. */
    long deleteByPortfolioAndDateGreaterThanEqual(Portfolio p, LocalDate from);
}
