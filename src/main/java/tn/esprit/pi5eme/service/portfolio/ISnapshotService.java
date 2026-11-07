package tn.esprit.pi5eme.service.portfolio;

import tn.esprit.pi5eme.entity.portfolio.Portfolio;
import tn.esprit.pi5eme.entity.portfolio.PortfolioSnapshot;

import java.time.LocalDate;

public interface ISnapshotService {

    /** Computes a snapshot without saving it. Used for the live "today" point of the performance chart. */
    PortfolioSnapshot buildSnapshot(Portfolio p, LocalDate date);

    /** Upsert by (portfolio, date). */
    PortfolioSnapshot takeSnapshot(Portfolio p, LocalDate date);

    /** Snapshots every portfolio; one failure is logged and the rest continue. Returns the number written. */
    int takeAllSnapshots(LocalDate date);
}
