package tn.esprit.pi5eme.controller.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.pi5eme.dto.ApiResponse;
import tn.esprit.pi5eme.dto.portfolio.LeaderboardEntry;
import tn.esprit.pi5eme.security.UserDetailsImpl;
import tn.esprit.pi5eme.service.portfolio.ILeaderboardService;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio/leaderboard")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class LeaderboardController {

    private final ILeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LeaderboardEntry>>> leaderboard(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Leaderboard retrieved",
                leaderboardService.getLeaderboard(principal.getId(), limit)));
    }
}
