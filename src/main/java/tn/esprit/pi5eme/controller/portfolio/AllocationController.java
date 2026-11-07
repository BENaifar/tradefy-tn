package tn.esprit.pi5eme.controller.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi5eme.dto.ApiResponse;
import tn.esprit.pi5eme.dto.portfolio.AllocationResponse;
import tn.esprit.pi5eme.dto.portfolio.AllocationTargetRequest;
import tn.esprit.pi5eme.dto.portfolio.AllocationTargetResponse;
import tn.esprit.pi5eme.dto.portfolio.RebalanceSuggestion;
import tn.esprit.pi5eme.security.UserDetailsImpl;
import tn.esprit.pi5eme.service.portfolio.AllocationGroupBy;
import tn.esprit.pi5eme.service.portfolio.IAllocationService;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio/allocation")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AllocationController {

    private final IAllocationService allocationService;

    @GetMapping
    public ResponseEntity<ApiResponse<AllocationResponse>> allocation(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(defaultValue = "INSTRUMENT") AllocationGroupBy by) {
        return ResponseEntity.ok(ApiResponse.success("Allocation retrieved",
                allocationService.getAllocation(principal.getId(), by)));
    }

    @GetMapping("/targets")
    public ResponseEntity<ApiResponse<List<AllocationTargetResponse>>> targets(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success("Targets retrieved", allocationService.getTargets(principal.getId())));
    }

    @PutMapping("/targets")
    public ResponseEntity<ApiResponse<List<AllocationTargetResponse>>> setTargets(
            @AuthenticationPrincipal UserDetailsImpl principal, @RequestBody List<AllocationTargetRequest> targets) {
        return ResponseEntity.ok(ApiResponse.success("Targets saved",
                allocationService.setTargets(principal.getId(), targets)));
    }

    @GetMapping("/rebalance")
    public ResponseEntity<ApiResponse<List<RebalanceSuggestion>>> rebalance(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success("Rebalance suggestions retrieved",
                allocationService.getRebalanceSuggestions(principal.getId())));
    }
}
