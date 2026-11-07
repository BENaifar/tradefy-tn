package tn.esprit.pi5eme.service.portfolio;

import org.junit.jupiter.api.Test;
import tn.esprit.pi5eme.service.portfolio.PnlCalculator.BuyResult;
import tn.esprit.pi5eme.service.portfolio.PnlCalculator.SellResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PnlCalculatorTest {

    private final PnlCalculator calc = new PnlCalculator();

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    @Test
    void referenceScenario() {
        BuyResult b1 = calc.applyBuy(0, BigDecimal.ZERO, 100, d("10.000"), d("4.000"));
        assertEquals(100, b1.newQuantity());
        assertEquals(d("10.040000"), b1.newAvgCost());
        assertEquals(d("1004.000"), b1.cashOut());

        BuyResult b2 = calc.applyBuy(100, b1.newAvgCost(), 50, d("12.000"), d("2.400"));
        assertEquals(150, b2.newQuantity());
        assertEquals(d("10.709333"), b2.newAvgCost());
        assertEquals(d("602.400"), b2.cashOut());

        SellResult s1 = calc.applySell(150, b2.newAvgCost(), 60, d("13.000"), d("3.120"));
        assertEquals(90, s1.newQuantity());
        assertEquals(d("134.320"), s1.realizedPnl());
        assertEquals(d("776.880"), s1.cashIn());

        SellResult s2 = calc.applySell(90, b2.newAvgCost(), 90, d("9.000"), d("3.240"));
        assertEquals(0, s2.newQuantity());
        assertEquals(d("-157.080"), s2.realizedPnl());
        assertEquals(d("806.760"), s2.cashIn());

        assertEquals(d("-22.760"), s1.realizedPnl().add(s2.realizedPnl()));
    }

    @Test
    void sellMoreThanHeldRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> calc.applySell(10, d("1.000000"), 11, d("1.000"), d("0.000")));
    }

    @Test
    void nonPositivePriceRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> calc.applyBuy(0, BigDecimal.ZERO, 1, BigDecimal.ZERO, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> calc.applyBuy(0, BigDecimal.ZERO, 1, d("-1"), BigDecimal.ZERO));
    }

    @Test
    void nonPositiveQuantityAndNegativeFeeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> calc.applyBuy(0, BigDecimal.ZERO, 0, d("1"), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> calc.applyBuy(0, BigDecimal.ZERO, 1, d("1"), d("-0.1")));
    }

    @Test
    void unrealizedNumbers() {
        BigDecimal avg = d("10.709333");
        assertEquals(d("43.600"), calc.unrealizedPnl(150, avg, d("11.000")));
        assertEquals(d("2.7141"), calc.unrealizedPnlPct(avg, d("11.000")));
    }

    @Test
    void zeroAvgCostGivesZeroPercent() {
        assertEquals(0, BigDecimal.ZERO.compareTo(calc.unrealizedPnlPct(BigDecimal.ZERO, d("5"))));
    }
}
