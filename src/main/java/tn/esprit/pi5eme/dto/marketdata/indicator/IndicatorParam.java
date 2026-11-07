package tn.esprit.pi5eme.dto.marketdata.indicator;

public record IndicatorParam(String name, String label, ParamType type, double defaultValue, double min, double max) {}
