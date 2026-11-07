package tn.esprit.pi5eme.controller.marketdata;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi5eme.dto.ApiResponse;
import tn.esprit.pi5eme.dto.marketdata.InstrumentResponse;
import tn.esprit.pi5eme.dto.marketdata.InstrumentUpdateRequest;
import tn.esprit.pi5eme.service.marketdata.IInstrumentService;

import java.util.List;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InstrumentController {

    private final IInstrumentService instrumentService;

    @GetMapping("/instruments")
    public ResponseEntity<ApiResponse<List<InstrumentResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sector) {
        return ResponseEntity.ok(ApiResponse.success("Instruments retrieved", instrumentService.search(q, sector)));
    }

    @GetMapping("/instruments/{ticker}")
    public ResponseEntity<ApiResponse<InstrumentResponse>> get(@PathVariable String ticker) {
        return ResponseEntity.ok(ApiResponse.success("Instrument retrieved", instrumentService.getByTicker(ticker)));
    }

    @GetMapping("/sectors")
    public ResponseEntity<ApiResponse<List<String>>> sectors() {
        return ResponseEntity.ok(ApiResponse.success("Sectors retrieved", instrumentService.getSectors()));
    }

    @PutMapping("/instruments/{ticker}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InstrumentResponse>> update(
            @PathVariable String ticker, @Valid @RequestBody InstrumentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Instrument updated", instrumentService.update(ticker, request)));
    }
}
