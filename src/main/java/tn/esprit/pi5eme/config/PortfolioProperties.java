package tn.esprit.pi5eme.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.portfolio")
@Getter
@Setter
public class PortfolioProperties {
    private BigDecimal initialCapital = new BigDecimal("100000.000");
    private BigDecimal feeRate = new BigDecimal("0.004");          // 0.4 %
    private BigDecimal minFee = new BigDecimal("0.000");
    private BigDecimal marketOrderBuffer = new BigDecimal("0.05"); // 5 %
    private BigDecimal maxCashOperation = new BigDecimal("1000000.000");
    private boolean devEndpointsEnabled = true;
}
