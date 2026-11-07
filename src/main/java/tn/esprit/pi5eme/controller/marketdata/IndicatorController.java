package tn.esprit.pi5eme.controller.marketdata;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi5eme.dto.ApiResponse;
import tn.esprit.pi5eme.dto.marketdata.CandleInterval;
import tn.esprit.pi5eme.dto.marketdata.indicator.IndicatorDefinition;
import tn.esprit.pi5eme.dto.marketdata.indicator.IndicatorResult;
import tn.esprit.pi5eme.service.marketdata.indicator.IIndicatorService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class IndicatorController {

    private final IIndicatorService indicatorService;

    @GetMapping("/indicators")
    public ResponseEntity<ApiResponse<List<IndicatorDefinition>>> catalog() {
        return ResponseEntity.ok(ApiResponse.success("Indicator catalog retrieved", indicatorService.getCatalog()));
    }

    /** Every query parameter other than interval, from and to is an indicator parameter (for example period=20). */
    @GetMapping("/instruments/{ticker}/indicators/{code}")
    public ResponseEntity<ApiResponse<IndicatorResult>> compute(
            @PathVariable String ticker, @PathVariable String code,
            @RequestParam(defaultValue = "D1") CandleInterval interval,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam Map<String, String> all) {
        Map<String, String> params = new HashMap<>(all);
        params.remove("interval");
        params.remove("from");
        params.remove("to");
        return ResponseEntity.ok(ApiResponse.success("Indicator computed",
                indicatorService.compute(ticker, code, interval, from, to, params)));
    }
}
