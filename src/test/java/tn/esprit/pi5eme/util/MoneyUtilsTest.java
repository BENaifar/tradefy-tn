package tn.esprit.pi5eme.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyUtilsTest {

    @Test
    void moneyRoundsUp() {
        assertEquals(new BigDecimal("1.235"), MoneyUtils.money(new BigDecimal("1.23456")));
    }

    @Test
    void moneyHalfEven() {
        assertEquals(new BigDecimal("1.234"), MoneyUtils.money(new BigDecimal("1.2345")));
    }

    @Test
    void percentOfQuarter() {
        assertEquals(new BigDecimal("25.0000"), MoneyUtils.percentOf(BigDecimal.ONE, new BigDecimal("4")));
    }

    @Test
    void percentOfZeroWhole() {
        assertEquals(0, BigDecimal.ZERO.compareTo(MoneyUtils.percentOf(BigDecimal.TEN, BigDecimal.ZERO)));
    }
}
