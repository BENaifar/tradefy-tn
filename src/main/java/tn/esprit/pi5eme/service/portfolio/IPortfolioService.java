package tn.esprit.pi5eme.service.portfolio;

import tn.esprit.pi5eme.dto.PageResponse;
import tn.esprit.pi5eme.dto.portfolio.CashOperationRequest;
import tn.esprit.pi5eme.dto.portfolio.PortfolioSummaryResponse;
import tn.esprit.pi5eme.dto.portfolio.TransactionResponse;
import tn.esprit.pi5eme.entity.portfolio.TransactionType;

import java.time.LocalDate;

public interface IPortfolioService {

    /** Creates the portfolio on first call. Valued with live prices. */
    PortfolioSummaryResponse getSummary(Long userId);

    /** Amount above maxCashOperation gives BadRequestException. */
    TransactionResponse deposit(Long userId, CashOperationRequest req);

    /** Amount above available cash gives InsufficientFundsException. */
    TransactionResponse withdraw(Long userId, CashOperationRequest req);

    /** Closes all positions and restores initial capital. Blocked while a reservation is active. */
    PortfolioSummaryResponse reset(Long userId);

    /** Filters are optional; size is clamped to 1..100. */
    PageResponse<TransactionResponse> getTransactions(Long userId, TransactionType type, String ticker,
                                                      LocalDate from, LocalDate to, int page, int size);
}
