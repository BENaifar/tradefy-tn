package tn.esprit.pi5eme.service.marketdata;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.pi5eme.dto.marketdata.CsvImportResult;
import tn.esprit.pi5eme.dto.marketdata.IntradayTickResponse;
import tn.esprit.pi5eme.dto.marketdata.PriceBarResponse;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.exception.ResourceNotFoundException;
import tn.esprit.pi5eme.repository.marketdata.InstrumentRepository;
import tn.esprit.pi5eme.repository.marketdata.IntradayTickRepository;
import tn.esprit.pi5eme.repository.marketdata.PriceBarRepository;
import tn.esprit.pi5eme.scheduler.MarketCalendar;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceHistoryServiceImpl implements IPriceHistoryService {

    private final InstrumentRepository instrumentRepository;
    private final PriceBarRepository priceBarRepository;
    private final IntradayTickRepository tickRepository;
    private final PriceHistoryCsvImporter csvImporter;
    private final MarketCalendar calendar;

    @Override
    @Transactional(readOnly = true)
    public List<PriceBarResponse> getDailyBars(String ticker, LocalDate from, LocalDate to) {
        requireInstrument(ticker);
        LocalDate end = to != null ? to : calendar.today();
        LocalDate start = from != null ? from : end.minusMonths(6);
        if (start.isAfter(end)) {
            throw new BadRequestException("'from' must not be after 'to'");
        }
        return priceBarRepository
                .findByInstrument_TickerIgnoreCaseAndDateBetweenOrderByDateAsc(ticker, start, end)
                .stream().map(PriceBarResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntradayTickResponse> getIntraday(String ticker, LocalDate date) {
        requireInstrument(ticker);
        LocalDate day = date != null ? date : calendar.today();
        Instant from = day.atStartOfDay(MarketCalendar.ZONE).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(MarketCalendar.ZONE).toInstant().minusMillis(1);
        return tickRepository
                .findByInstrument_TickerIgnoreCaseAndTimestampBetweenOrderByTimestampAsc(ticker, from, to)
                .stream().map(IntradayTickResponse::fromEntity).toList();
    }

    @Override
    public CsvImportResult importCsv(MultipartFile file) {
        return csvImporter.importCsv(file);
    }

    private void requireInstrument(String ticker) {
        if (instrumentRepository.findByTickerIgnoreCase(ticker).isEmpty()) {
            throw new ResourceNotFoundException("Instrument not found: " + ticker);
        }
    }
}
