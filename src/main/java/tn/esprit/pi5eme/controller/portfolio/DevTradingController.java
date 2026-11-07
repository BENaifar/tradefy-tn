package tn.esprit.pi5eme.controller.portfolio;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi5eme.dto.ApiResponse;
import tn.esprit.pi5eme.dto.portfolio.DevExecutionRequest;
import tn.esprit.pi5eme.dto.portfolio.PositionResponse;
import tn.esprit.pi5eme.security.UserDetailsImpl;
import tn.esprit.pi5eme.service.portfolio.IDevTradingService;

/**
 * Temporary stand-in for the missing trading module: instant fills so the portfolio can be used meanwhile.
 * Delete when the trading module calls IPortfolioTradingGateway.
 */
@RestController
@RequestMapping("/api/portfolio/dev")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@ConditionalOnProperty(name = "app.portfolio.dev-endpoints-enabled", havingValue = "true", matchIfMissing = true)
public class DevTradingController {

    private final IDevTradingService devTradingService;

    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<PositionResponse>> execute(
            @AuthenticationPrincipal UserDetailsImpl principal, @Valid @RequestBody DevExecutionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Execution applied",
                devTradingService.execute(principal.getId(), request)));
    }
}
