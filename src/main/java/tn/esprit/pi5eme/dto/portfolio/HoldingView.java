package tn.esprit.pi5eme.dto.portfolio;

import java.math.BigDecimal;

/** Read-only view of an open position for the risk module. */
public record HoldingView(String ticker, String sector, long quantity, BigDecimal avgCost,
                          BigDecimal lastPrice, BigDecimal marketValue, BigDecimal weightPct) {}
