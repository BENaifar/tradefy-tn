package tn.esprit.pi5eme.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tn.esprit.pi5eme.dto.portfolio.DevExecutionRequest;
import tn.esprit.pi5eme.dto.portfolio.PositionResponse;
import tn.esprit.pi5eme.dto.portfolio.TradeExecution;
import tn.esprit.pi5eme.entity.portfolio.OrderSide;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Temporary stand-in for the missing trading module: an instant fill that still goes through the real
 * reserve and execute path of the gateway. Delete when the trading module calls IPortfolioTradingGateway.
 */
@Service
@ConditionalOnProperty(name = "app.portfolio.dev-endpoints-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DevTradingServiceImpl implements IDevTradingService {

    private final IPortfolioTradingGateway gateway;
    private final IMarketDataService marketDataService;
    private final IPositionService positionService;

    @Override
    public PositionResponse execute(Long userId, DevExecutionRequest req) {
        String ticker = req.getTicker().trim().toUpperCase();
        BigDecimal price = req.getPrice() != null
                ? MoneyUtils.money(req.getPrice())
                : marketDataService.getLastPrice(ticker);
        String orderId = "DEV-" + UUID.randomUUID();
        if (req.getSide() == OrderSide.BUY) {
            gateway.reserveForBuy(userId, orderId, ticker, req.getQuantity(), price);
        } else {
            gateway.reserveForSell(userId, orderId, ticker, req.getQuantity());
        }
        try {
            gateway.applyExecution(new TradeExecution(userId, orderId, UUID.randomUUID().toString(), ticker,
                    req.getSide(), req.getQuantity(), price, Instant.now()));
        } catch (RuntimeException e) {
            gateway.releaseReservation(orderId);
            throw e;
        }
        gateway.releaseReservation(orderId);
        log.info("Dev execution: user={} {} {} {} @ {}", userId, req.getSide(), req.getQuantity(), ticker, price);
        return positionService.getPosition(userId, ticker).getPosition();
    }
}
