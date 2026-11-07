package tn.esprit.pi5eme.repository.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;
import tn.esprit.pi5eme.entity.portfolio.Position;
import tn.esprit.pi5eme.entity.portfolio.PositionStatus;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByPortfolioAndTickerAndStatus(Portfolio p, String ticker, PositionStatus s);

    List<Position> findByPortfolioAndStatusOrderByTickerAsc(Portfolio p, PositionStatus s);

    List<Position> findByPortfolioAndStatusOrderByClosedAtDesc(Portfolio p, PositionStatus s);

    List<Position> findByPortfolioAndTickerOrderByOpenedAtDesc(Portfolio p, String ticker);
}
