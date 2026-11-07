package tn.esprit.pi5eme.dto.marketdata.indicator;

import java.util.List;

/** What the frontend needs to build a picker: parameters, outputs and where to draw. */
public record IndicatorDefinition(String code, String name, String description,
                                  IndicatorPlacement placement,
                                  List<IndicatorParam> params,
                                  List<IndicatorOutput> outputs,
                                  List<Double> referenceLines) {}
