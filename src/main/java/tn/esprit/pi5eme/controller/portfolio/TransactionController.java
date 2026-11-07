package tn.esprit.pi5eme.controller.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi5eme.dto.ApiResponse;
import tn.esprit.pi5eme.dto.PageResponse;
import tn.esprit.pi5eme.dto.portfolio.TransactionResponse;
import tn.esprit.pi5eme.entity.portfolio.TransactionType;
import tn.esprit.pi5eme.security.UserDetailsImpl;
import tn.esprit.pi5eme.service.portfolio.IPortfolioService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/portfolio/transactions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TransactionController {

    private final IPortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> list(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved",
                portfolioService.getTransactions(principal.getId(), type, ticker, from, to, page, size)));
    }
}
