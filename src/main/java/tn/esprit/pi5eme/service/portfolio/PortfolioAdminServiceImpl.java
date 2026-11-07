package tn.esprit.pi5eme.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.portfolio.AdminPortfolioRow;
import tn.esprit.pi5eme.entity.User;
import tn.esprit.pi5eme.repository.UserRepository;
import tn.esprit.pi5eme.repository.portfolio.PortfolioTransactionRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioAdminServiceImpl implements IPortfolioAdminService {

    private final PortfolioStatsCalculator stats;
    private final PortfolioTransactionRepository transactionRepository;
    // Read-only use of the user module repository, as allowed by the implementation plan.
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminPortfolioRow> listPortfolios() {
        List<PortfolioStatsCalculator.PortfolioStat> all = stats.computeAll();
        Map<Long, User> users = userRepository
                .findAllById(all.stream().map(s -> s.portfolio().getUserId()).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return all.stream().map(s -> {
            User u = users.get(s.portfolio().getUserId());
            return AdminPortfolioRow.builder()
                    .userId(s.portfolio().getUserId())
                    .email(u == null ? null : u.getEmail())
                    .fullName(u == null ? null : u.getFirstName() + " " + u.getLastName())
                    .totalValue(s.totalValue()).totalReturnPct(s.totalReturnPct())
                    .openPositions(s.openPositions())
                    .lastActivity(transactionRepository.findTopByPortfolioOrderByExecutedAtDesc(s.portfolio())
                            .map(t -> t.getExecutedAt()).orElse(null))
                    .build();
        }).sorted(Comparator.comparing(AdminPortfolioRow::getUserId)).toList();
    }
}
