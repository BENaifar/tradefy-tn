package tn.esprit.pi5eme.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.config.PortfolioProperties;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;

/** fee = max(gross x feeRate, minFee), rounded to money; gross <= 0 gives 0. */
@Component
@RequiredArgsConstructor
public class PercentageFeePolicy implements FeePolicy {

    private final PortfolioProperties props;

    @Override
    public BigDecimal feeFor(BigDecimal grossAmount) {
        if (grossAmount == null || grossAmount.signum() <= 0) {
            return MoneyUtils.money(BigDecimal.ZERO);
        }
        BigDecimal fee = MoneyUtils.money(grossAmount.multiply(props.getFeeRate()));
        BigDecimal min = MoneyUtils.money(props.getMinFee());
        return fee.max(min);
    }
}
