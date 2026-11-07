package tn.esprit.pi5eme.service.portfolio;

import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;

/** Stateless P&L maths. No repositories. */
@Component
public class PnlCalculator {

    /** cashOut = gross + fee, positive. */
    public record BuyResult(long newQuantity, BigDecimal newAvgCost, BigDecimal cashOut) {}

    /** cashIn = gross - fee, positive. */
    public record SellResult(long newQuantity, BigDecimal realizedPnl, BigDecimal cashIn) {}

    public BuyResult applyBuy(long qty, BigDecimal avgCost, long fillQty, BigDecimal price, BigDecimal fee) {
        validate(fillQty, price, fee);
        BigDecimal gross = MoneyUtils.money(price.multiply(BigDecimal.valueOf(fillQty)));
        BigDecimal totalCost = avgCost.multiply(BigDecimal.valueOf(qty))
                .add(price.multiply(BigDecimal.valueOf(fillQty)))
                .add(fee);
        long newQty = qty + fillQty;
        BigDecimal newAvg = MoneyUtils.cost(totalCost.divide(BigDecimal.valueOf(newQty), 10, MoneyUtils.RM));
        return new BuyResult(newQty, newAvg, MoneyUtils.money(gross.add(fee)));
    }

    public SellResult applySell(long qty, BigDecimal avgCost, long fillQty, BigDecimal price, BigDecimal fee) {
        validate(fillQty, price, fee);
        if (fillQty > qty) {
            throw new IllegalArgumentException("Cannot sell " + fillQty + " shares, only " + qty + " held");
        }
        BigDecimal gross = MoneyUtils.money(price.multiply(BigDecimal.valueOf(fillQty)));
        BigDecimal realized = MoneyUtils.money(
                BigDecimal.valueOf(fillQty).multiply(price.subtract(avgCost)).subtract(fee));
        return new SellResult(qty - fillQty, realized, MoneyUtils.money(gross.subtract(fee)));
    }

    public BigDecimal unrealizedPnl(long qty, BigDecimal avgCost, BigDecimal lastPrice) {
        return MoneyUtils.money(BigDecimal.valueOf(qty).multiply(lastPrice.subtract(avgCost)));
    }

    public BigDecimal unrealizedPnlPct(BigDecimal avgCost, BigDecimal lastPrice) {
        if (avgCost == null || avgCost.signum() == 0) {
            return MoneyUtils.percent(BigDecimal.ZERO);
        }
        return MoneyUtils.percent(lastPrice.subtract(avgCost)
                .divide(avgCost, 10, MoneyUtils.RM).multiply(MoneyUtils.HUNDRED));
    }

    private static void validate(long fillQty, BigDecimal price, BigDecimal fee) {
        if (fillQty <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        if (fee == null || fee.signum() < 0) {
            throw new IllegalArgumentException("Fee must not be negative");
        }
    }
}
