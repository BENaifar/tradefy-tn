package tn.esprit.pi5eme.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;
import tn.esprit.pi5eme.entity.portfolio.Position;
import tn.esprit.pi5eme.entity.portfolio.PositionStatus;
import tn.esprit.pi5eme.repository.portfolio.PortfolioRepository;
import tn.esprit.pi5eme.repository.portfolio.PositionRepository;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.util.*;

/** Values every portfolio with one single price lookup. Used by the admin list and the leaderboard. */
@Component
@RequiredArgsConstructor
public class PortfolioStatsCalculator {

    public record PortfolioStat(Portfolio portfolio, BigDecimal totalValue, BigDecimal totalReturnPct,
                                int openPositions) {}

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final IMarketDataService marketDataService;

    public List<PortfolioStat> computeAll() {
        List<Portfolio> portfolios = portfolioRepository.findAll();
        Map<Long, List<Position>> byPortfolio = new HashMap<>();
        Set<String> tickers = new HashSet<>();
        for (Portfolio p : portfolios) {
            List<Position> open = positionRepository.findByPortfolioAndStatusOrderByTickerAsc(p, PositionStatus.OPEN);
            byPortfolio.put(p.getId(), open);
            open.forEach(pos -> tickers.add(pos.getTicker()));
        }
        Map<String, BigDecimal> prices = tickers.isEmpty() ? Map.of() : marketDataService.getLastPrices(tickers);

        List<PortfolioStat> result = new ArrayList<>();
        for (Portfolio p : portfolios) {
            List<Position> open = byPortfolio.get(p.getId());
            BigDecimal marketValue = BigDecimal.ZERO;
            for (Position pos : open) {
                BigDecimal price = prices.getOrDefault(pos.getTicker().toUpperCase(Locale.ROOT), pos.getAvgCost());
                marketValue = marketValue.add(price.multiply(BigDecimal.valueOf(pos.getQuantity())));
            }
            BigDecimal total = MoneyUtils.money(p.getCashBalance().add(marketValue));
            BigDecimal returnPct = MoneyUtils.percentOf(total.subtract(p.getNetContributions()), p.getNetContributions());
            result.add(new PortfolioStat(p, total, returnPct, open.size()));
        }
        return result;
    }
}
