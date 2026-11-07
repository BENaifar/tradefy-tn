package tn.esprit.pi5eme.service.portfolio;

import tn.esprit.pi5eme.dto.portfolio.LeaderboardEntry;

import java.util.List;

public interface ILeaderboardService {

    /** Top portfolios by total return, best first. Limit is clamped to 1..100. No emails are exposed. */
    List<LeaderboardEntry> getLeaderboard(Long callerUserId, int limit);
}
