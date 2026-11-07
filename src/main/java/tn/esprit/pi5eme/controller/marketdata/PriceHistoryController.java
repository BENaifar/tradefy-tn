package tn.esprit.pi5eme.controller.marketdata;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi5eme.dto.ApiResponse;
import tn.esprit.pi5eme.dto.marketdata.CandleInterval;
import tn.esprit.pi5eme.dto.marketdata.CandleSeriesResponse;
import tn.esprit.pi5eme.dto.marketdata.IntradayTickResponse;
import tn.esprit.pi5eme.dto.marketdata.PriceBarResponse;
import tn.esprit.pi5eme.service.marketdata.ICandleService;
import tn.esprit.pi5eme.service.marketdata.IPriceHistoryService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/market/instruments/{ticker}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PriceHistoryController {

    private final IPriceHistoryService historyService;
    private final ICandleService candleService;

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PriceBarResponse>>> history(
            @PathVariable String ticker,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success("History retrieved", historyService.getDailyBars(ticker, from, to)));
    }

    @GetMapping("/intraday")
    public ResponseEntity<ApiResponse<List<IntradayTickResponse>>> intraday(
            @PathVariable String ticker,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success("Intraday ticks retrieved", historyService.getIntraday(ticker, date)));
    }

    @GetMapping("/candles")
    public ResponseEntity<ApiResponse<CandleSeriesResponse>> candles(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "D1") CandleInterval interval,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success("Candles retrieved",
                candleService.getSeries(ticker, interval, from, to)));
    }
}
