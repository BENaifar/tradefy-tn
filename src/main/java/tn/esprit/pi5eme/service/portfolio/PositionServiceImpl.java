package tn.esprit.pi5eme.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.marketdata.InstrumentResponse;
import tn.esprit.pi5eme.dto.marketdata.QuoteResponse;
import tn.esprit.pi5eme.dto.portfolio.PositionDetailResponse;
import tn.esprit.pi5eme.dto.portfolio.PositionResponse;
import tn.esprit.pi5eme.dto.portfolio.TransactionResponse;
import tn.esprit.pi5eme.entity.portfolio.Portfolio;
import tn.esprit.pi5eme.entity.portfolio.Position;
import tn.esprit.pi5eme.entity.portfolio.PositionStatus;
import tn.esprit.pi5eme.exception.ResourceNotFoundException;
import tn.esprit.pi5eme.repository.portfolio.PortfolioTransactionRepository;
import tn.esprit.pi5eme.repository.portfolio.PositionRepository;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PositionServiceImpl implements IPositionService {

    private static final String UNCLASSIFIED = "Non classé";

    private final PortfolioProvisioner provisioner;
    private final PositionRepository positionRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final PortfolioValuator valuator;
    private final PnlCalculator calc;
    private final IMarketDataService marketDataService;
    private final Clock clock;

    @Override
    @Transactional
    public List<PositionResponse> getOpenPositions(Long userId) {
        Portfolio p = provisioner.getOrCreate(userId);
        List<Position> open = positionRepository.findByPortfolioAndStatusOrderByTickerAsc(p, PositionStatus.OPEN);
        return toResponses(p, open, true);
    }

    @Override
    @Transactional
    public List<PositionResponse> getClosedPositions(Long userId) {
        Portfolio p = provisioner.getOrCreate(userId);
        List<Position> closed = positionRepository.findByPortfolioAndStatusOrderByClosedAtDesc(p, PositionStatus.CLOSED);
        return toResponses(p, closed, false);
    }

    @Override
    @Transactional
    public PositionDetailResponse getPosition(Long userId, String ticker) {
        Portfolio p = provisioner.getOrCreate(userId);
        String t = ticker.trim().toUpperCase(Locale.ROOT);
        Position position = positionRepository.findByPortfolioAndTickerAndStatus(p, t, PositionStatus.OPEN)
                .orElseGet(() -> positionRepository.findByPortfolioAndTickerOrderByOpenedAtDesc(p, t).stream()
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("No position found for " + t)));
        boolean live = position.getStatus() == PositionStatus.OPEN;
        PositionResponse response = toResponses(p, List.of(position), live).get(0);
        List<TransactionResponse> txs = transactionRepository
                .findByPortfolioAndTickerOrderByExecutedAtDesc(p, t).stream()
                .map(TransactionResponse::fromEntity).toList();
        return PositionDetailResponse.builder().position(response).transactions(txs).build();
    }

    private List<PositionResponse> toResponses(Portfolio p, List<Position> positions, boolean live) {
        if (positions.isEmpty()) {
            return List.of();
        }
        List<String> tickers = positions.stream().map(Position::getTicker).distinct().toList();
        Map<String, InstrumentResponse> instruments = marketDataService.getInstruments(tickers);
        Map<String, QuoteResponse> quotes = live ? marketDataService.getQuotes(tickers) : Map.of();
        BigDecimal totalValue = live
                ? valuator.value(p, positionRepository.findByPortfolioAndStatusOrderByTickerAsc(p, PositionStatus.OPEN))
                        .totalValue()
                : null;
        Instant now = Instant.now(clock);

        return positions.stream().map(pos -> {
            String key = pos.getTicker().toUpperCase(Locale.ROOT);
            InstrumentResponse ins = instruments.get(key);
            QuoteResponse q = quotes.get(key);
            BigDecimal last = null;
            BigDecimal marketValue = null;
            BigDecimal unrealized = null;
            BigDecimal unrealizedPct = null;
            BigDecimal weight = null;
            if (live) {
                last = (q != null && q.getLast() != null) ? q.getLast() : pos.getAvgCost();
                marketValue = MoneyUtils.money(last.multiply(BigDecimal.valueOf(pos.getQuantity())));
                unrealized = calc.unrealizedPnl(pos.getQuantity(), pos.getAvgCost(), last);
                unrealizedPct = calc.unrealizedPnlPct(pos.getAvgCost(), last);
                weight = MoneyUtils.percentOf(marketValue, totalValue);
            }
            Instant end = pos.getClosedAt() != null ? pos.getClosedAt() : now;
            long days = pos.getOpenedAt() == null ? 0 : Duration.between(pos.getOpenedAt(), end).toDays();
            return PositionResponse.builder()
                    .id(pos.getId()).ticker(pos.getTicker())
                    .name(ins == null ? null : ins.getName())
                    .sector(ins == null || ins.getSector() == null ? UNCLASSIFIED : ins.getSector())
                    .status(pos.getStatus()).quantity(pos.getQuantity())
                    .reservedQuantity(pos.getReservedQuantity()).availableQuantity(pos.getAvailableQuantity())
                    .avgCost(pos.getAvgCost()).totalCost(pos.getTotalCost())
                    .lastPrice(last).marketValue(marketValue)
                    .unrealizedPnl(unrealized).unrealizedPnlPct(unrealizedPct)
                    .dayChangePct(q == null ? null : q.getChangePercent())
                    .realizedPnl(pos.getRealizedPnl()).weightPct(weight)
                    .openedAt(pos.getOpenedAt()).closedAt(pos.getClosedAt()).holdingDays(days)
                    .build();
        }).toList();
    }
}
