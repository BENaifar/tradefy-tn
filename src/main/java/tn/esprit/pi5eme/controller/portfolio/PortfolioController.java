package tn.esprit.pi5eme.controller.portfolio;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi5eme.dto.ApiResponse;
import tn.esprit.pi5eme.dto.portfolio.BuyingPower;
import tn.esprit.pi5eme.dto.portfolio.CashOperationRequest;
import tn.esprit.pi5eme.dto.portfolio.PortfolioSummaryResponse;
import tn.esprit.pi5eme.dto.portfolio.TransactionResponse;
import tn.esprit.pi5eme.security.UserDetailsImpl;
import tn.esprit.pi5eme.service.portfolio.IPortfolioService;
import tn.esprit.pi5eme.service.portfolio.IPortfolioTradingGateway;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PortfolioController {

    private final IPortfolioService portfolioService;
    private final IPortfolioTradingGateway gateway;

    @GetMapping
    public ResponseEntity<ApiResponse<PortfolioSummaryResponse>> summary(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success("Portfolio retrieved", portfolioService.getSummary(principal.getId())));
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @AuthenticationPrincipal UserDetailsImpl principal, @Valid @RequestBody CashOperationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Deposit recorded", portfolioService.deposit(principal.getId(), request)));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @AuthenticationPrincipal UserDetailsImpl principal, @Valid @RequestBody CashOperationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Withdrawal recorded", portfolioService.withdraw(principal.getId(), request)));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<PortfolioSummaryResponse>> reset(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success("Portfolio reset", portfolioService.reset(principal.getId())));
    }

    @GetMapping("/buying-power")
    public ResponseEntity<ApiResponse<BuyingPower>> buyingPower(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success("Buying power retrieved", gateway.getBuyingPower(principal.getId())));
    }
}
