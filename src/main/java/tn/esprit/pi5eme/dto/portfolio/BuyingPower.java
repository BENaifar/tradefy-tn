package tn.esprit.pi5eme.dto.portfolio;

import java.math.BigDecimal;
import java.util.Map;

/** availableShares: ticker to (quantity - reservedQuantity) for open positions. */
public record BuyingPower(Long userId, BigDecimal cashBalance, BigDecimal reservedCash,
                          BigDecimal availableCash, Map<String, Long> availableShares) {}
