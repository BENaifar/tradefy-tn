package tn.esprit.pi5eme.service.portfolio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.marketdata.InstrumentResponse;
import tn.esprit.pi5eme.dto.portfolio.HoldingView;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;
import tn.esprit.pi5eme.entity.portfolio.Position;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.repository.portfolio.PositionRepository;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class PortfolioQueryServiceImplTest {

    private static final long USER = 71L;

    @MockBean IMarketDataService marketData;
    @MockBean MarketDataProvider provider;
    @Autowired IPortfolioQueryService service;
    @Autowired PortfolioProvisioner provisioner;
    @Autowired PositionRepository positions;

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    @Test
    void holdingWeightsPlusCashWeightIs100() {
        Portfolio p = provisioner.getOrCreate(USER);
        p.setCashBalance(d("1000.000"));
        positions.save(Position.builder().portfolio(p).ticker("AAA").quantity(100).avgCost(d("30.000000"))
                .openedAt(Instant.now()).build());
        positions.save(Position.builder().portfolio(p).ticker("BBB").quantity(100).avgCost(d("10.000000"))
                .openedAt(Instant.now()).build());
        when(marketData.getLastPrices(anyCollection())).thenReturn(Map.of("AAA", d("30"), "BBB", d("10")));
        when(marketData.getInstruments(anyCollection())).thenReturn(Map.of(
                "AAA", InstrumentResponse.builder().ticker("AAA").sector("Banques").build()));

        List<HoldingView> holdings = service.getHoldings(USER);
        BigDecimal total = service.getPortfolioValue(USER);
        BigDecimal cashWeight = new BigDecimal("1000").multiply(new BigDecimal("100")).divide(total, 4,
                java.math.RoundingMode.HALF_EVEN);
        BigDecimal sum = holdings.stream().map(HoldingView::weightPct).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(2, holdings.size());
        assertEquals(d("5000.000"), total);
        assertEquals(0, new BigDecimal("100").compareTo(sum.add(cashWeight)));
        assertEquals("Non classé", holdings.get(1).sector());
    }
}
