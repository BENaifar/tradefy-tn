package tn.esprit.pi5eme.repository.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.pi5eme.entity.portfolio.CashReservation;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;
import tn.esprit.pi5eme.entity.portfolio.ReservationStatus;

import java.util.List;
import java.util.Optional;

public interface CashReservationRepository extends JpaRepository<CashReservation, Long> {

    Optional<CashReservation> findByOrderId(String orderId);

    List<CashReservation> findByPortfolioAndStatus(Portfolio p, ReservationStatus s);
}
