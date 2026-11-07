package tn.esprit.pi5eme.dto.portfolio;

import tn.esprit.pi5eme.entity.portfolio.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;

/** A fill reported by the trading module. orderId and executionId may be null (dev executions). */
public record TradeExecution(Long userId, String orderId, String executionId, String ticker,
                             OrderSide side, long quantity, BigDecimal price, Instant executedAt) {}
