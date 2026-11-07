package tn.esprit.pi5eme.service.portfolio;

import java.math.BigDecimal;

public interface FeePolicy {

    /** Fee for a trade of the given gross amount. Zero for a gross amount of zero or less. */
    BigDecimal feeFor(BigDecimal grossAmount);
}
