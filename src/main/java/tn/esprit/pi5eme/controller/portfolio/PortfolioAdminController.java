package tn.esprit.pi5eme.controller.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi5eme.dto.ApiResponse;
import tn.esprit.pi5eme.dto.portfolio.AdminPortfolioRow;
import tn.esprit.pi5eme.dto.portfolio.PortfolioSummaryResponse;
import tn.esprit.pi5eme.scheduler.MarketCalendar;
import tn.esprit.pi5eme.service.portfolio.IPortfolioAdminService;
import tn.esprit.pi5eme.service.portfolio.IPortfolioService;
import tn.esprit.pi5eme.service.portfolio.ISnapshotService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PortfolioAdminController {

    private final ISnapshotService snapshotService;
    private final IPortfolioAdminService adminService;
    private final IPortfolioService portfolioService;
    private final MarketCalendar calendar;

    @GetMapping("/portfolios")
    public ResponseEntity<ApiResponse<List<AdminPortfolioRow>>> portfolios() {
        return ResponseEntity.ok(ApiResponse.success("Portfolios retrieved", adminService.listPortfolios()));
    }

    @GetMapping("/portfolios/{userId}")
    public ResponseEntity<ApiResponse<PortfolioSummaryResponse>> portfolio(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Portfolio retrieved", portfolioService.getSummary(userId)));
    }

    /** Manual snapshot run, for demos and for days the app was down. Defaults to today. */
    @PostMapping("/snapshots")
    public ResponseEntity<ApiResponse<Integer>> snapshots(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate day = date != null ? date : calendar.today();
        return ResponseEntity.ok(ApiResponse.success("Snapshots taken", snapshotService.takeAllSnapshots(day)));
    }
}
