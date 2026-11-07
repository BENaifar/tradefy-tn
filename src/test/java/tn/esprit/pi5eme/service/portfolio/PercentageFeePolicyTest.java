package tn.esprit.pi5eme.service.portfolio;

import org.junit.jupiter.api.Test;
import tn.esprit.pi5eme.config.PortfolioProperties;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PercentageFeePolicyTest {

    private PercentageFeePolicy policy(String minFee) {
        PortfolioProperties p = new PortfolioProperties();
        p.setFeeRate(new BigDecimal("0.004"));
        p.setMinFee(new BigDecimal(minFee));
        return new PercentageFeePolicy(p);
    }

    @Test
    void thousandGivesFour() {
        assertEquals(new BigDecimal("4.000"), policy("0").feeFor(new BigDecimal("1000")));
    }

    @Test
    void zeroGivesZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(policy("0").feeFor(BigDecimal.ZERO)));
    }

    @Test
    void minimumFeeApplies() {
        PercentageFeePolicy p = policy("1.000");
        assertEquals(new BigDecimal("1.000"), p.feeFor(new BigDecimal("100")));
        assertEquals(new BigDecimal("4.000"), p.feeFor(new BigDecimal("1000")));
    }
}
