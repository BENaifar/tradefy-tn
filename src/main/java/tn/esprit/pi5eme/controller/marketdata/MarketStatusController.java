package tn.esprit.pi5eme.controller.marketdata;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.pi5eme.dto.ApiResponse;
import tn.esprit.pi5eme.dto.marketdata.MarketStatusResponse;
import tn.esprit.pi5eme.service.marketdata.IMarketStatusService;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MarketStatusController {

    private final IMarketStatusService statusService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<MarketStatusResponse>> status() {
        return ResponseEntity.ok(ApiResponse.success("Market status retrieved", statusService.getStatus()));
    }
}
