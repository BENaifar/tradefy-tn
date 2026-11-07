package tn.esprit.pi5eme.repository.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;
import tn.esprit.pi5eme.entity.portfolio.PortfolioTransaction;
import tn.esprit.pi5eme.entity.portfolio.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Filtered, paged search uses JpaSpecificationExecutor (safe with nullable filters on PostgreSQL). */
public interface PortfolioTransactionRepository
        extends JpaRepository<PortfolioTransaction, Long>, JpaSpecificationExecutor<PortfolioTransaction> {

    List<PortfolioTransaction> findByPortfolioAndTickerOrderByExecutedAtDesc(Portfolio p, String ticker);

    @Query("select coalesce(sum(t.netAmount), 0) from PortfolioTransaction t "
            + "where t.portfolio = :p and t.type in :types and t.executedAt >= :from and t.executedAt < :to")
    BigDecimal sumNetAmount(@Param("p") Portfolio p, @Param("types") Collection<TransactionType> types,
                            @Param("from") Instant from, @Param("to") Instant to);

    boolean existsByExecutionId(String executionId);

    Optional<PortfolioTransaction> findTopByPortfolioOrderByExecutedAtDesc(Portfolio p);

    Optional<PortfolioTransaction> findTopByPortfolioAndTypeOrderByExecutedAtDesc(Portfolio p, TransactionType type);

    @Query("select count(t) from PortfolioTransaction t where t.portfolio = :p and t.type = :type "
            + "and t.realizedPnl > 0 and t.executedAt >= :since")
    long countWinning(@Param("p") Portfolio p, @Param("type") TransactionType type, @Param("since") Instant since);

    @Query("select count(t) from PortfolioTransaction t where t.portfolio = :p and t.type = :type "
            + "and t.realizedPnl < 0 and t.executedAt >= :since")
    long countLosing(@Param("p") Portfolio p, @Param("type") TransactionType type, @Param("since") Instant since);
}
