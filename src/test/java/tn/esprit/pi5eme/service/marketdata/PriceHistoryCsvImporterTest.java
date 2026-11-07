package tn.esprit.pi5eme.service.marketdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import tn.esprit.pi5eme.dto.marketdata.CsvImportResult;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.repository.marketdata.*;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PriceHistoryCsvImporterTest {

    @MockBean MarketDataProvider provider;
    @Autowired PriceHistoryCsvImporter importer;
    @Autowired InstrumentRepository instruments;
    @Autowired PriceBarRepository bars;
    @Autowired WatchlistItemRepository watchlist;
    @Autowired IntradayTickRepository ticks;
    @Autowired QuoteRepository quotes;

    @BeforeEach
    void clean() {
        watchlist.deleteAll();
        bars.deleteAll();
        ticks.deleteAll();
        quotes.deleteAll();
        instruments.deleteAll();
        instruments.save(Instrument.builder().ticker("TJARI").isin("TN0000000010").name("ATTIJARI").build());
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "h.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private static final String VALID = """
            date,ticker,open,high,low,close,volume
            2026-10-01,TJARI,48.100,48.900,47.950,48.500,15230
            2026-10-02,TJARI,48.500,49.000,48.000,48.800,12000
            2026-10-03,TJARI,48.800,49.500,48.700,49.200,9000
            """;

    @Test
    void importThenReimport() {
        CsvImportResult first = importer.importCsv(csv(VALID));
        assertEquals(3, first.getInserted());
        CsvImportResult second = importer.importCsv(csv(VALID));
        assertEquals(3, second.getUpdated());
        assertEquals(0, second.getInserted());
        assertEquals(3, bars.count());
    }

    @Test
    void unknownTickerIsReportedOthersImported() {
        String content = VALID + "2026-10-04,XYZ,1,1,1,1,1\n";
        CsvImportResult r = importer.importCsv(csv(content));
        assertEquals(3, r.getInserted());
        assertEquals(1, r.getErrors().size());
        assertTrue(r.getErrors().get(0).contains("unknown ticker XYZ"));
    }

    @Test
    void semicolonWithDecimalCommas() {
        String content = "date;ticker;open;high;low;close;volume\n2026-10-01;TJARI;48,100;48,900;47,950;48,500;15230\n";
        CsvImportResult r = importer.importCsv(csv(content));
        assertEquals(1, r.getInserted());
        assertEquals(0, new java.math.BigDecimal("48.500").compareTo(bars.findAll().get(0).getClose()));
    }

    @Test
    void missingHeaderRejected() {
        assertThrows(BadRequestException.class,
                () -> importer.importCsv(csv("2026-10-01,TJARI,1,1,1,1,1\n")));
    }
}
