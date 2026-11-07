package tn.esprit.pi5eme.service.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.marketdata.InstrumentResponse;
import tn.esprit.pi5eme.dto.portfolio.AllocationResponse;
import tn.esprit.pi5eme.dto.portfolio.AllocationSlice;
import tn.esprit.pi5eme.dto.portfolio.AllocationTargetRequest;
import tn.esprit.pi5eme.dto.portfolio.RebalanceSuggestion;
import tn.esprit.pi5eme.entity.portfolio.*;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.exception.ResourceNotFoundException;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.repository.portfolio.PositionRepository;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Setup: cash 1000, A 100 @ 30 (Banques) = 3000, B 100 @ 10 (Assurances) = 1000. Total 5000. */
@SpringBootTest
@Transactional
class AllocationServiceImplTest {

    private static final long USER = 31L;

    @MockBean IMarketDataService marketData;
    @MockBean MarketDataProvider provider;
    @Autowired IAllocationService service;
    @Autowired PortfolioProvisioner provisioner;
    @Autowired PositionRepository positions;

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    @BeforeEach
    void setUp() {
        Portfolio p = provisioner.getOrCreate(USER);
        p.setCashBalance(d("1000.000"));
        addPosition(p, "AAA", 100, "30");
        addPosition(p, "BBB", 100, "10");

        when(marketData.getLastPrices(anyCollection())).thenReturn(Map.of("AAA", d("30"), "BBB", d("10")));
        when(marketData.getInstruments(anyCollection())).thenReturn(Map.of(
                "AAA", InstrumentResponse.builder().ticker("AAA").sector("Banques").build(),
                "BBB", InstrumentResponse.builder().ticker("BBB").sector("Assurances").build()));
        when(marketData.getInstrument(anyString())).thenAnswer(inv -> {
            String t = inv.getArgument(0);
            if (t.equals("NOPE")) {
                throw new ResourceNotFoundException("no " + t);
            }
            return InstrumentResponse.builder().ticker(t).build();
        });
        when(marketData.getSectors()).thenReturn(List.of("Assurances", "Banques"));
    }

    private void addPosition(Portfolio p, String ticker, long qty, String avg) {
        positions.save(Position.builder().portfolio(p).ticker(ticker).quantity(qty)
                .avgCost(new BigDecimal(avg).setScale(6)).openedAt(Instant.now()).build());
    }

    private AllocationTargetRequest target(TargetType type, String key, String pct) {
        return AllocationTargetRequest.builder().type(type).key(key).percent(d(pct)).build();
    }

    private AllocationSlice slice(AllocationResponse r, String key) {
        return r.getSlices().stream().filter(s -> s.getKey().equals(key)).findFirst().orElseThrow();
    }

    @Test
    void byInstrument() {
        AllocationResponse r = service.getAllocation(USER, AllocationGroupBy.INSTRUMENT);
        assertEquals(d("5000.000"), r.getTotalValue());
        assertEquals(d("60.0000"), slice(r, "AAA").getWeightPct());
        assertEquals(d("20.0000"), slice(r, "BBB").getWeightPct());
        assertEquals(d("20.0000"), slice(r, "CASH").getWeightPct());
        assertEquals("AAA", r.getSlices().get(0).getKey());
    }

    @Test
    void bySector() {
        AllocationResponse r = service.getAllocation(USER, AllocationGroupBy.SECTOR);
        assertEquals(d("60.0000"), slice(r, "Banques").getWeightPct());
        assertEquals(d("20.0000"), slice(r, "Assurances").getWeightPct());
        assertEquals(d("20.0000"), slice(r, "CASH").getWeightPct());
    }

    @Test
    void nullSectorIsNonClasse() {
        when(marketData.getInstruments(anyCollection())).thenReturn(Map.of(
                "AAA", InstrumentResponse.builder().ticker("AAA").build(),
                "BBB", InstrumentResponse.builder().ticker("BBB").sector("Assurances").build()));
        AllocationResponse r = service.getAllocation(USER, AllocationGroupBy.SECTOR);
        assertEquals(d("60.0000"), slice(r, "Non classé").getWeightPct());
    }

    @Test
    void rebalanceWithTargets() {
        service.setTargets(USER, List.of(target(TargetType.INSTRUMENT, "AAA", "40"),
                target(TargetType.INSTRUMENT, "BBB", "40")));
        List<RebalanceSuggestion> s = service.getRebalanceSuggestions(USER);
        RebalanceSuggestion a = s.stream().filter(x -> x.getTicker().equals("AAA")).findFirst().orElseThrow();
        RebalanceSuggestion b = s.stream().filter(x -> x.getTicker().equals("BBB")).findFirst().orElseThrow();
        assertEquals(OrderSide.SELL, a.getSide());
        assertEquals(33, a.getQuantity());
        assertEquals(OrderSide.BUY, b.getSide());
        assertEquals(100, b.getQuantity());
    }

    @Test
    void targetsSummingOver100Rejected() {
        assertThrows(BadRequestException.class, () -> service.setTargets(USER, List.of(
                target(TargetType.INSTRUMENT, "AAA", "60"), target(TargetType.INSTRUMENT, "BBB", "50"))));
    }

    @Test
    void mixedTypesRejected() {
        assertThrows(BadRequestException.class, () -> service.setTargets(USER, List.of(
                target(TargetType.INSTRUMENT, "AAA", "30"), target(TargetType.SECTOR, "Banques", "30"))));
    }

    @Test
    void unknownTickerRejected() {
        assertThrows(BadRequestException.class, () -> service.setTargets(USER,
                List.of(target(TargetType.INSTRUMENT, "NOPE", "10"))));
    }

    @Test
    void heldTickerWithoutTargetIsSoldEntirely() {
        service.setTargets(USER, List.of(target(TargetType.INSTRUMENT, "AAA", "60")));
        List<RebalanceSuggestion> s = service.getRebalanceSuggestions(USER);
        RebalanceSuggestion b = s.stream().filter(x -> x.getTicker().equals("BBB")).findFirst().orElseThrow();
        assertEquals(OrderSide.SELL, b.getSide());
        assertEquals(100, b.getQuantity());
    }

    @Test
    void replacingTargetsKeepsOnlyNewOnes() {
        service.setTargets(USER, List.of(target(TargetType.INSTRUMENT, "AAA", "50")));
        service.setTargets(USER, List.of(target(TargetType.INSTRUMENT, "AAA", "20")));
        assertEquals(1, service.getTargets(USER).size());
        assertEquals(d("20.0000"), service.getTargets(USER).get(0).getPercent());
    }
}
