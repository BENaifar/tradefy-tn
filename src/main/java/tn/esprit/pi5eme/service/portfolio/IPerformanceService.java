package tn.esprit.pi5eme.service.portfolio;

import tn.esprit.pi5eme.dto.portfolio.PerformanceResponse;

public interface IPerformanceService {

    /**
     * Equity curve and statistics for a range. Snapshots before the last RESET are ignored, and a live
     * point for today is appended when today has no snapshot yet.
     */
    PerformanceResponse getPerformance(Long userId, PerformanceRange range);
}
