package tn.esprit.pi5eme.service.marketdata.indicator;

import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.dto.marketdata.indicator.IndicatorDefinition;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Holds every {@link TechnicalIndicator} bean. Fails fast when two indicators share a code. */
@Component
public class IndicatorRegistry {

    private final Map<String, TechnicalIndicator> byCode = new HashMap<>();

    public IndicatorRegistry(List<TechnicalIndicator> indicators) {
        for (TechnicalIndicator indicator : indicators) {
            String code = indicator.definition().code().toUpperCase(Locale.ROOT);
            if (byCode.put(code, indicator) != null) {
                throw new IllegalStateException("Duplicate indicator code: " + code);
            }
        }
    }

    /** Case-insensitive lookup. */
    public Optional<TechnicalIndicator> find(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(byCode.get(code.toUpperCase(Locale.ROOT)));
    }

    /** All definitions, sorted by code. */
    public List<IndicatorDefinition> catalog() {
        return byCode.values().stream().map(TechnicalIndicator::definition)
                .sorted(Comparator.comparing(IndicatorDefinition::code)).toList();
    }
}
