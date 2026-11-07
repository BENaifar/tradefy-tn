package tn.esprit.pi5eme.service.portfolio;

import org.junit.jupiter.api.Test;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;
import tn.esprit.pi5eme.entity.portfolio.Position;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortfolioValuatorTest {

    private Portfolio portfolio() {
        BigDecimal c = new BigDecimal("1000.000");
        return Portfolio.builder().userId(1L).initialCapital(c).cashBalance(c).netContributions(c).build();
    }

    private Position pos(String t, long q, String avg) {
        return Position.builder().ticker(t).quantity(q).avgCost(new BigDecimal(avg)).build();
    }

    @Test
    void twoPositionsTotal() {
        IMarketDataService md = mock(IMarketDataService.class);
        when(md.getLastPrices(anyCollection())).thenReturn(
                Map.of("AAA", new BigDecimal("11.000"), "BBB", new BigDecimal("20.000")));
        PortfolioValuator.Valuation v = new PortfolioValuator(md)
                .value(portfolio(), List.of(pos("AAA", 10, "10.000000"), pos("BBB", 5, "18.000000")));
        assertEquals(new BigDecimal("210.000"), v.marketValue());
        assertEquals(new BigDecimal("1210.000"), v.totalValue());
        assertEquals(0, v.missingPrices().size());
    }

    @Test
    void missingPriceFallsBackToAvgCost() {
        IMarketDataService md = mock(IMarketDataService.class);
        when(md.getLastPrices(anyCollection())).thenReturn(Map.of("AAA", new BigDecimal("11.000")));
        PortfolioValuator.Valuation v = new PortfolioValuator(md)
                .value(portfolio(), List.of(pos("AAA", 10, "10.000000"), pos("BBB", 5, "18.000000")));
        assertEquals(new BigDecimal("200.000"), v.marketValue());
        assertEquals(List.of("BBB"), v.missingPrices());
    }
}
