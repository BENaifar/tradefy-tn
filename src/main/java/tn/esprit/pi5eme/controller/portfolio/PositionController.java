package tn.esprit.pi5eme.controller.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi5eme.dto.ApiResponse;
import tn.esprit.pi5eme.dto.portfolio.PositionDetailResponse;
import tn.esprit.pi5eme.dto.portfolio.PositionResponse;
import tn.esprit.pi5eme.entity.portfolio.PositionStatus;
import tn.esprit.pi5eme.security.UserDetailsImpl;
import tn.esprit.pi5eme.service.portfolio.IPositionService;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio/positions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PositionController {

    private final IPositionService positionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PositionResponse>>> list(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(defaultValue = "OPEN") PositionStatus status) {
        List<PositionResponse> data = status == PositionStatus.OPEN
                ? positionService.getOpenPositions(principal.getId())
                : positionService.getClosedPositions(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Positions retrieved", data));
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<ApiResponse<PositionDetailResponse>> one(
            @AuthenticationPrincipal UserDetailsImpl principal, @PathVariable String ticker) {
        return ResponseEntity.ok(ApiResponse.success("Position retrieved",
                positionService.getPosition(principal.getId(), ticker)));
    }
}
