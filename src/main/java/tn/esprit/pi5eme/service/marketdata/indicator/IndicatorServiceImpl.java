package tn.esprit.pi5eme.service.marketdata.indicator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.marketdata.Candle;
import tn.esprit.pi5eme.dto.marketdata.CandleInterval;
import tn.esprit.pi5eme.dto.marketdata.indicator.*;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.exception.ResourceNotFoundException;
import tn.esprit.pi5eme.scheduler.MarketCalendar;
import tn.esprit.pi5eme.service.marketdata.ICandleService;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IndicatorServiceImpl implements IIndicatorService {

    private final IndicatorRegistry registry;
    private final ICandleService candleService;
    private final MarketCalendar calendar;

    @Override
    public List<IndicatorDefinition> getCatalog() {
        return registry.catalog();
    }

    @Override
    @Transactional(readOnly = true)
    public IndicatorResult compute(String ticker, String code, CandleInterval interval, LocalDate from, LocalDate to,
                                   Map<String, String> rawParams) {
        TechnicalIndicator indicator = registry.find(code)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown indicator: " + code));
        IndicatorDefinition def = indicator.definition();
        CandleInterval iv = interval != null ? interval : CandleInterval.D1;

        Map<String, Double> params = resolveParams(def, rawParams == null ? Map.of() : rawParams);
        indicator.validate(params);

        LocalDate end = to != null ? to : calendar.today();
        LocalDate start = from != null ? from : candleService.defaultFrom(iv, end);
        List<Candle> candles = candleService.getCandlesWithLookback(ticker, iv, start, end, indicator.warmup(params));
        Map<String, double[]> values = indicator.compute(candles, params);

        long cutoff = start.atStartOfDay(MarketCalendar.ZONE).toEpochSecond();
        Map<String, List<IndicatorPoint>> series = new LinkedHashMap<>();
        for (IndicatorOutput output : def.outputs()) {
            double[] v = values.get(output.key());
            List<IndicatorPoint> points = new ArrayList<>();
            for (int i = 0; i < candles.size(); i++) {
                Candle c = candles.get(i);
                if (c.time() < cutoff) {
                    continue;
                }
                double x = v == null || i >= v.length ? Double.NaN : v[i];
                BigDecimal value = Double.isNaN(x) || Double.isInfinite(x) ? null
                        : MoneyUtils.percent(BigDecimal.valueOf(x));
                points.add(new IndicatorPoint(c.time(), value));
            }
            series.put(output.key(), points);
        }
        return new IndicatorResult(def.code(), params, series);
    }

    private Map<String, Double> resolveParams(IndicatorDefinition def, Map<String, String> raw) {
        Set<String> known = new HashSet<>();
        for (IndicatorParam p : def.params()) {
            known.add(p.name());
        }
        for (String name : raw.keySet()) {
            if (!known.contains(name)) {
                throw new BadRequestException("Unknown parameter '" + name + "' for " + def.code()
                        + ". Allowed: " + known);
            }
        }
        Map<String, Double> resolved = new LinkedHashMap<>();
        for (IndicatorParam p : def.params()) {
            String text = raw.get(p.name());
            double value;
            if (text == null || text.isBlank()) {
                value = p.defaultValue();
            } else {
                try {
                    value = Double.parseDouble(text.trim());
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Parameter '" + p.name() + "' must be a number, got '" + text + "'");
                }
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    throw new BadRequestException("Parameter '" + p.name() + "' must be a finite number");
                }
            }
            if (p.type() == ParamType.INT && value != Math.rint(value)) {
                throw new BadRequestException("Parameter '" + p.name() + "' must be a whole number, got " + value);
            }
            if (value < p.min() || value > p.max()) {
                throw new BadRequestException("Parameter '" + p.name() + "' must be between " + p.min()
                        + " and " + p.max() + ", got " + value);
            }
            resolved.put(p.name(), value);
        }
        return resolved;
    }
}
