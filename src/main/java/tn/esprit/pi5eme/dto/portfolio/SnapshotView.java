package tn.esprit.pi5eme.dto.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Read-only view of a daily snapshot for the risk module. */
public record SnapshotView(LocalDate date, BigDecimal totalValue, BigDecimal dailyReturnPct,
                           BigDecimal cumulativeReturnPct) {}
