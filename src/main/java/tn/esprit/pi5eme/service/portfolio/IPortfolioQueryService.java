package tn.esprit.pi5eme.service.portfolio;

import tn.esprit.pi5eme.dto.portfolio.HoldingView;
import tn.esprit.pi5eme.dto.portfolio.SnapshotView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only contract for the future risk module. It never changes portfolio state and has no controller.
 * Risk metrics (VaR, volatility, Sharpe, stop-loss) belong to the risk module, not here.
 */
public interface IPortfolioQueryService {

    /** Open positions with live prices; weightPct is relative to the total portfolio value (cash included). */
    List<HoldingView> getHoldings(Long userId);

    /**
     * Daily snapshots between two dates, oldest first. Null from means the beginning of time,
     * null to means today.
     */
    List<SnapshotView> getSnapshots(Long userId, LocalDate from, LocalDate to);

    /** Current total value: cash plus open positions at last prices. */
    BigDecimal getPortfolioValue(Long userId);
}
