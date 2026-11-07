package tn.esprit.pi5eme.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.portfolio.LeaderboardEntry;
import tn.esprit.pi5eme.entity.User;
import tn.esprit.pi5eme.repository.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements ILeaderboardService {

    private final PortfolioStatsCalculator stats;
    // Read-only use of the user module repository, as allowed by the implementation plan.
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getLeaderboard(Long callerUserId, int limit) {
        int effective = Math.max(1, Math.min(100, limit));
        List<PortfolioStatsCalculator.PortfolioStat> sorted = stats.computeAll().stream()
                .sorted(Comparator.comparing(PortfolioStatsCalculator.PortfolioStat::totalReturnPct).reversed())
                .limit(effective).toList();
        Map<Long, User> users = userRepository
                .findAllById(sorted.stream().map(s -> s.portfolio().getUserId()).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<LeaderboardEntry> result = new ArrayList<>();
        int rank = 1;
        for (PortfolioStatsCalculator.PortfolioStat s : sorted) {
            Long userId = s.portfolio().getUserId();
            result.add(LeaderboardEntry.builder().rank(rank++).displayName(displayName(users.get(userId)))
                    .totalReturnPct(s.totalReturnPct()).totalValue(s.totalValue())
                    .me(userId.equals(callerUserId)).build());
        }
        return result;
    }

    private static String displayName(User u) {
        if (u == null) {
            return "Anonymous";
        }
        String last = u.getLastName() == null || u.getLastName().isBlank() ? "" : " " + u.getLastName().charAt(0) + ".";
        return u.getFirstName() + last;
    }
}
