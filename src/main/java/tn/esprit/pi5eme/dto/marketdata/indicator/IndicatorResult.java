package tn.esprit.pi5eme.dto.marketdata.indicator;

import java.util.List;
import java.util.Map;

/** series key is IndicatorOutput.key. */
public record IndicatorResult(String code, Map<String, Double> params, Map<String, List<IndicatorPoint>> series) {}
