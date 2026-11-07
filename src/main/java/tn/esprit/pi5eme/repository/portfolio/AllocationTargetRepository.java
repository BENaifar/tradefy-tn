package tn.esprit.pi5eme.repository.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import tn.esprit.pi5eme.entity.portfolio.AllocationTarget;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;

import java.util.List;

public interface AllocationTargetRepository extends JpaRepository<AllocationTarget, Long> {

    List<AllocationTarget> findByPortfolio(Portfolio p);

    @Modifying
    void deleteByPortfolio(Portfolio p);
}
