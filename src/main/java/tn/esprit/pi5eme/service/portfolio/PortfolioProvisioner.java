package tn.esprit.pi5eme.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.config.PortfolioProperties;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;
import tn.esprit.pi5eme.entity.portfolio.PortfolioTransaction;
import tn.esprit.pi5eme.entity.portfolio.TransactionType;
import tn.esprit.pi5eme.repository.portfolio.PortfolioRepository;
import tn.esprit.pi5eme.repository.portfolio.PortfolioTransactionRepository;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

/** Every portfolio service starts with getOrCreate(userId). */
@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioProvisioner {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final PortfolioProperties props;
    private final Clock clock;

    @Transactional
    public Portfolio getOrCreate(Long userId) {
        return portfolioRepository.findByUserId(userId).orElseGet(() -> create(userId));
    }

    private Portfolio create(Long userId) {
        BigDecimal capital = MoneyUtils.money(props.getInitialCapital());
        Portfolio p = portfolioRepository.save(Portfolio.builder()
                .userId(userId)
                .initialCapital(capital)
                .cashBalance(capital)
                .netContributions(capital)
                .build());
        transactionRepository.save(PortfolioTransaction.builder()
                .portfolio(p)
                .type(TransactionType.INITIAL_DEPOSIT)
                .grossAmount(capital)
                .netAmount(capital)
                .cashBalanceAfter(capital)
                .note("Initial capital")
                .executedAt(Instant.now(clock))
                .build());
        log.info("Portfolio created for user {} with {} TND", userId, capital);
        return p;
    }
}
