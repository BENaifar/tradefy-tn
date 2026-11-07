package tn.esprit.pi5eme.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.pi5eme.dto.marketdata.RawQuote;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BvmtQuoteMapperTest {

    private static final LocalDate FALLBACK = LocalDate.of(2020, 1, 1);
    private final BvmtQuoteMapper mapper = new BvmtQuoteMapper();
    private BvmtMarketResponse response;

    @BeforeEach
    void load() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/bvmt/market.json")) {
            response = new ObjectMapper().readValue(in, BvmtMarketResponse.class);
        }
    }

    private RawQuote find(List<RawQuote> l, String ticker) {
        return l.stream().filter(q -> q.ticker().equals(ticker)).findFirst().orElseThrow();
    }

    @Test
    void mapsAllFour() {
        assertEquals(4, mapper.map(response, FALLBACK).size());
    }

    @Test
    void mapsAmenBank() {
        RawQuote ab = find(mapper.map(response, FALLBACK), "AB");
        assertEquals(new BigDecimal("92.300"), ab.last());
        assertEquals(new BigDecimal("92.350"), ab.previousClose());
        assertEquals(new BigDecimal("-0.0500"), ab.changePercent());
        assertNull(ab.open());
        assertEquals(new BigDecimal("92.490"), ab.high());
        assertEquals(new BigDecimal("910051.600"), ab.turnover());
        assertEquals(new BigDecimal("94.140"), ab.upperLimit());
        assertEquals(new BigDecimal("92.300"), ab.bid());
        assertEquals(120L, ab.bidQty());
        assertEquals(LocalDate.of(2026, 11, 6), ab.sessionDate());
        assertEquals(LocalTime.of(15, 0), ab.quoteTime());
    }

    @Test
    void roundsLongTurnover() {
        assertEquals(new BigDecimal("30434.850"), find(mapper.map(response, FALLBACK), "ARTES").turnover());
    }

    @Test
    void zeroBecomesNull() {
        RawQuote a = find(mapper.map(response, FALLBACK), "AETEC");
        assertEquals(0L, a.volume());
        assertNull(a.bid());
        assertNull(a.upperLimit());
    }

    @Test
    void badSeanceFallsBack() {
        response.getMarkets().get(0).setSeance("xx");
        assertEquals(FALLBACK, find(mapper.map(response, FALLBACK), "AETEC").sessionDate());
    }

    @Test
    void nullReferentielSkipped() {
        response.getMarkets().add(new BvmtMarketResponse.Item());
        BvmtMarketResponse.Item bad = response.getMarkets().get(4);
        bad.setLast(BigDecimal.ONE);
        response.getMarkets().remove(3);
        assertEquals(3, mapper.map(response, FALLBACK).size());
    }

    @Test
    void parsesFrenchMonths() {
        String[][] cases = {{"15 févr. 2026", "2026-02-15"}, {"3 août 2026", "2026-08-03"},
                {"1 déc. 2026", "2026-12-01"}, {"9 mai 2026", "2026-05-09"}};
        for (String[] c : cases) {
            response.getMarkets().get(0).setSeance(c[0]);
            assertEquals(LocalDate.parse(c[1]), find(mapper.map(response, FALLBACK), "AETEC").sessionDate(), c[0]);
        }
    }
}
