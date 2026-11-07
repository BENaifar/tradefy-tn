package tn.esprit.pi5eme.service.marketdata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.pi5eme.dto.marketdata.InstrumentResponse;
import tn.esprit.pi5eme.dto.marketdata.InstrumentUpdateRequest;
import tn.esprit.pi5eme.dto.marketdata.MoverType;
import tn.esprit.pi5eme.dto.marketdata.QuoteResponse;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.Quote;
import tn.esprit.pi5eme.repository.marketdata.InstrumentRepository;
import tn.esprit.pi5eme.repository.marketdata.QuoteRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstrumentServiceImplTest {

    @Mock InstrumentRepository instruments;
    @Mock QuoteRepository quotes;
    @InjectMocks InstrumentServiceImpl service;

    private Quote quote(String ticker, String pct, long volume, String turnover) {
        Instrument i = Instrument.builder().ticker(ticker).isin("X" + ticker).name(ticker).build();
        return Quote.builder().instrument(i).last(BigDecimal.TEN).changePercent(new BigDecimal(pct))
                .volume(volume).turnover(new BigDecimal(turnover)).build();
    }

    private List<Quote> sample() {
        return List.of(
                quote("A", "1.5000", 10, "100"),
                quote("B", "-2.0000", 20, "500"),
                quote("C", "3.0000", 0, "0"),
                quote("D", "0.5000", 5, "300"));
    }

    @Test
    void gainersOrderedAndExcludeZeroVolume() {
        when(quotes.findAllActiveWithInstrument()).thenReturn(sample());
        List<QuoteResponse> r = service.getMovers(MoverType.GAINERS, 10);
        assertEquals(List.of("A", "D", "B"), r.stream().map(QuoteResponse::getTicker).toList());
    }

    @Test
    void losersOrdered() {
        when(quotes.findAllActiveWithInstrument()).thenReturn(sample());
        List<QuoteResponse> r = service.getMovers(MoverType.LOSERS, 2);
        assertEquals(List.of("B", "D"), r.stream().map(QuoteResponse::getTicker).toList());
    }

    @Test
    void activeByTurnover() {
        when(quotes.findAllActiveWithInstrument()).thenReturn(sample());
        List<QuoteResponse> r = service.getMovers(MoverType.ACTIVE, 2);
        assertEquals(List.of("B", "D"), r.stream().map(QuoteResponse::getTicker).toList());
    }

    @Test
    void limitClampedTo50() {
        List<Quote> many = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            many.add(quote("T" + i, "1.0000", 1, "1"));
        }
        when(quotes.findAllActiveWithInstrument()).thenReturn(many);
        assertEquals(50, service.getMovers(MoverType.GAINERS, 500).size());
    }

    @Test
    void updateWithNullSectorKeepsOldSector() {
        Instrument i = Instrument.builder().ticker("AB").isin("X").name("AB").sector("Banques").build();
        when(instruments.findByTickerIgnoreCase("AB")).thenReturn(Optional.of(i));
        when(instruments.save(any(Instrument.class))).thenAnswer(inv -> inv.getArgument(0));
        InstrumentResponse r = service.update("AB", InstrumentUpdateRequest.builder().active(false).build());
        assertEquals("Banques", r.getSector());
        assertEquals(false, r.isActive());
    }
}
