package tn.esprit.pi5eme.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi5eme.entity.portfolio.PortfolioTransaction;
import tn.esprit.pi5eme.entity.portfolio.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private TransactionType type;
    private String ticker;
    private Long quantity;
    private BigDecimal price;
    private BigDecimal grossAmount;
    private BigDecimal fee;
    private BigDecimal netAmount;
    private BigDecimal realizedPnl;
    private BigDecimal cashBalanceAfter;
    private String orderId;
    private String executionId;
    private String note;
    private Instant executedAt;

    public static TransactionResponse fromEntity(PortfolioTransaction t) {
        return TransactionResponse.builder()
                .id(t.getId()).type(t.getType()).ticker(t.getTicker()).quantity(t.getQuantity())
                .price(t.getPrice()).grossAmount(t.getGrossAmount()).fee(t.getFee())
                .netAmount(t.getNetAmount()).realizedPnl(t.getRealizedPnl())
                .cashBalanceAfter(t.getCashBalanceAfter()).orderId(t.getOrderId())
                .executionId(t.getExecutionId()).note(t.getNote()).executedAt(t.getExecutedAt())
                .build();
    }
}
