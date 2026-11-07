package tn.esprit.pi5eme.dto.marketdata.indicator;

import java.math.BigDecimal;

/** value null means not computable yet (warm-up). */
public record IndicatorPoint(long time, BigDecimal value) {}
