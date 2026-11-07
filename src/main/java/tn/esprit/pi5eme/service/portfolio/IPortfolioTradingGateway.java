package tn.esprit.pi5eme.service.portfolio;

import tn.esprit.pi5eme.dto.portfolio.BuyingPower;
import tn.esprit.pi5eme.dto.portfolio.TradeExecution;

import java.math.BigDecimal;

/** Contract used by the future trading module. */
public interface IPortfolioTradingGateway {

    /**
     * Lock cash for a BUY order. limitPrice null means a market order (last price x (1 + marketOrderBuffer)).
     * Locks quantity x price + estimated fee.
     *
     * @throws tn.esprit.pi5eme.exception.InsufficientFundsException if available cash is too low
     * @throws tn.esprit.pi5eme.exception.BadRequestException        if the orderId is already reserved
     */
    void reserveForBuy(Long userId, String orderId, String ticker, long quantity, BigDecimal limitPrice);

    /**
     * Lock shares for a SELL order.
     *
     * @throws tn.esprit.pi5eme.exception.InsufficientHoldingsException if there is no position or not enough available shares
     */
    void reserveForSell(Long userId, String orderId, String ticker, long quantity);

    /**
     * Release whatever is still reserved for this order (cancel, expiry, after the last partial fill).
     * No-op if already CONSUMED or RELEASED.
     *
     * @throws tn.esprit.pi5eme.exception.ResourceNotFoundException if the orderId is unknown
     */
    void releaseReservation(String orderId);

    /** Apply a fill: updates cash, position and ledger, and consumes the order reservation if one exists. */
    void applyExecution(TradeExecution execution);

    /** Cash and share availability of the user. */
    BuyingPower getBuyingPower(Long userId);
}
