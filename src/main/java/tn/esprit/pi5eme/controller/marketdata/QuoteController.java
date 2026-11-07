package tn.esprit.pi5eme.controller.marketdata;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi5eme.dto.ApiResponse;
import tn.esprit.pi5eme.dto.marketdata.MoverType;
import tn.esprit.pi5eme.dto.marketdata.QuoteResponse;
import tn.esprit.pi5eme.service.marketdata.IInstrumentService;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;

import java.util.List;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class QuoteController {

    private final IInstrumentService instrumentService;
    private final IMarketDataService marketDataService;

    @GetMapping("/quotes")
    public ResponseEntity<ApiResponse<List<QuoteResponse>>> all() {
        return ResponseEntity.ok(ApiResponse.success("Quotes retrieved", instrumentService.getAllQuotes()));
    }

    @GetMapping("/quotes/{ticker}")
    public ResponseEntity<ApiResponse<QuoteResponse>> one(@PathVariable String ticker) {
        return ResponseEntity.ok(ApiResponse.success("Quote retrieved", marketDataService.getQuote(ticker)));
    }

    @GetMapping("/movers")
    public ResponseEntity<ApiResponse<List<QuoteResponse>>> movers(
            @RequestParam(defaultValue = "GAINERS") MoverType type,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Movers retrieved", instrumentService.getMovers(type, limit)));
    }
}
