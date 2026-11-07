package tn.esprit.pi5eme.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {
    public static final int MONEY_SCALE = 3;
    public static final int COST_SCALE = 6;
    public static final int PERCENT_SCALE = 4;
    public static final RoundingMode RM = RoundingMode.HALF_EVEN;
    public static final BigDecimal HUNDRED = new BigDecimal("100");

    private MoneyUtils() {}

    public static BigDecimal money(BigDecimal v)   { return v == null ? null : v.setScale(MONEY_SCALE, RM); }
    public static BigDecimal cost(BigDecimal v)    { return v == null ? null : v.setScale(COST_SCALE, RM); }
    public static BigDecimal percent(BigDecimal v) { return v == null ? null : v.setScale(PERCENT_SCALE, RM); }
    public static boolean isPositive(BigDecimal v) { return v != null && v.signum() > 0; }

    /** (part / whole) * 100, or ZERO if whole is zero. */
    public static BigDecimal percentOf(BigDecimal part, BigDecimal whole) {
        if (part == null || whole == null || whole.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return percent(part.divide(whole, 10, RM).multiply(HUNDRED));
    }

    public static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
