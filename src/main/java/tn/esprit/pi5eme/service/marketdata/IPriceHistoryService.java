package tn.esprit.pi5eme.service.marketdata;

import org.springframework.web.multipart.MultipartFile;
import tn.esprit.pi5eme.dto.marketdata.CsvImportResult;
import tn.esprit.pi5eme.dto.marketdata.IntradayTickResponse;
import tn.esprit.pi5eme.dto.marketdata.PriceBarResponse;

import java.time.LocalDate;
import java.util.List;

public interface IPriceHistoryService {

    /** Defaults: to = today, from = to minus 6 months. from after to gives BadRequestException. */
    List<PriceBarResponse> getDailyBars(String ticker, LocalDate from, LocalDate to);

    /** Default date = today. Window is the full day in Africa/Tunis. */
    List<IntradayTickResponse> getIntraday(String ticker, LocalDate date);

    CsvImportResult importCsv(MultipartFile file);
}
