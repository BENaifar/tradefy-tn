package tn.esprit.pi5eme.controller.marketdata;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi5eme.dto.ApiResponse;
import tn.esprit.pi5eme.dto.marketdata.WatchlistItemResponse;
import tn.esprit.pi5eme.security.UserDetailsImpl;
import tn.esprit.pi5eme.service.marketdata.IWatchlistService;

import java.util.List;

@RestController
@RequestMapping("/api/market/watchlist")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WatchlistController {

    private final IWatchlistService watchlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WatchlistItemResponse>>> list(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success("Watchlist retrieved", watchlistService.list(principal.getId())));
    }

    @PostMapping("/{ticker}")
    public ResponseEntity<ApiResponse<WatchlistItemResponse>> add(
            @AuthenticationPrincipal UserDetailsImpl principal, @PathVariable String ticker) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Added to watchlist", watchlistService.add(principal.getId(), ticker)));
    }

    @DeleteMapping("/{ticker}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @AuthenticationPrincipal UserDetailsImpl principal, @PathVariable String ticker) {
        watchlistService.remove(principal.getId(), ticker);
        return ResponseEntity.ok(ApiResponse.success("Removed from watchlist"));
    }
}
