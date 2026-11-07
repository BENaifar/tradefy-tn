package tn.esprit.pi5eme.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;
import tn.esprit.pi5eme.entity.portfolio.Position;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioValuator {

    public record Valuation(BigDecimal cash, BigDecimal marketValue, BigDecimal totalValue,
                            Map<String, BigDecimal> pricesUsed, List<String> missingPrices) {}

    private final IMarketDataService marketDataService;

    /** Values open positions with one price lookup; tickers without a price are valued at avgCost. */
    public Valuation value(Portfolio p, List<Position> openPositions) {
        List<String> tickers = openPositions.stream().map(Position::getTicker).toList();
        Map<String, BigDecimal> prices = tickers.isEmpty() ? Map.of() : marketDataService.getLastPrices(tickers);

        Map<String, BigDecimal> used = new HashMap<>();
        List<String> missing = new ArrayList<>();
        BigDecimal marketValue = BigDecimal.ZERO;
        for (Position pos : openPositions) {
            BigDecimal price = prices.get(pos.getTicker().toUpperCase());
            if (price == null) {
                log.warn("No price for {}, valuing at average cost", pos.getTicker());
                missing.add(pos.getTicker());
                price = pos.getAvgCost();
            }
            used.put(pos.getTicker(), price);
            marketValue = marketValue.add(price.multiply(BigDecimal.valueOf(pos.getQuantity())));
        }
        marketValue = MoneyUtils.money(marketValue);
        BigDecimal cash = MoneyUtils.money(p.getCashBalance());
        return new Valuation(cash, marketValue, MoneyUtils.money(cash.add(marketValue)), used, missing);
    }
}
