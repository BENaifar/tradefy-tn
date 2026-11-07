package tn.esprit.pi5eme.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.config.PortfolioProperties;
import tn.esprit.pi5eme.dto.portfolio.BuyingPower;
import tn.esprit.pi5eme.dto.portfolio.TradeExecution;
import tn.esprit.pi5eme.entity.portfolio.*;
import tn.esprit.pi5eme.event.PortfolioChangedEvent;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.exception.InsufficientFundsException;
import tn.esprit.pi5eme.exception.InsufficientHoldingsException;
import tn.esprit.pi5eme.exception.ResourceNotFoundException;
import tn.esprit.pi5eme.repository.portfolio.CashReservationRepository;
import tn.esprit.pi5eme.repository.portfolio.PortfolioRepository;
import tn.esprit.pi5eme.repository.portfolio.PortfolioTransactionRepository;
import tn.esprit.pi5eme.repository.portfolio.PositionRepository;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioTradingGatewayImpl implements IPortfolioTradingGateway {

    private final PortfolioProvisioner provisioner;
    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final CashReservationRepository reservationRepository;
    private final IMarketDataService marketDataService;
    private final FeePolicy feePolicy;
    private final PnlCalculator calc;
    private final PortfolioProperties props;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    @Override
    @Transactional
    public void reserveForBuy(Long userId, String orderId, String ticker, long quantity, BigDecimal limitPrice) {
        requireOrderAndQuantity(orderId, quantity);
        String t = normalize(ticker);
        marketDataService.getInstrument(t);
        Portfolio portfolio = provisioner.getOrCreate(userId);

        BigDecimal price = limitPrice != null
                ? MoneyUtils.money(limitPrice)
                : MoneyUtils.money(marketDataService.getLastPrice(t)
                        .multiply(BigDecimal.ONE.add(props.getMarketOrderBuffer())));
        if (price.signum() <= 0) {
            throw new BadRequestException("Price must be positive");
        }
        BigDecimal gross = MoneyUtils.money(price.multiply(BigDecimal.valueOf(quantity)));
        BigDecimal amount = MoneyUtils.money(gross.add(feePolicy.feeFor(gross)));

        if (amount.compareTo(portfolio.getAvailableCash()) > 0) {
            throw new InsufficientFundsException("Insufficient funds: required " + amount
                    + " TND, available " + portfolio.getAvailableCash() + " TND");
        }
        if (reservationRepository.findByOrderId(orderId).isPresent()) {
            throw new BadRequestException("Order " + orderId + " already has a reservation");
        }
        reservationRepository.save(CashReservation.builder()
                .portfolio(portfolio).orderId(orderId).ticker(t).side(OrderSide.BUY)
                .reservedAmount(amount).limitPrice(limitPrice == null ? null : price).build());
        portfolio.setReservedCash(portfolio.getReservedCash().add(amount));
        portfolioRepository.save(portfolio);
        log.info("Reserved {} TND for BUY order {} ({} {})", amount, orderId, quantity, t);
    }

    @Override
    @Transactional
    public void reserveForSell(Long userId, String orderId, String ticker, long quantity) {
        requireOrderAndQuantity(orderId, quantity);
        String t = normalize(ticker);
        Portfolio portfolio = provisioner.getOrCreate(userId);
        Position position = positionRepository
                .findByPortfolioAndTickerAndStatus(portfolio, t, PositionStatus.OPEN)
                .orElseThrow(() -> new InsufficientHoldingsException("No open position in " + t));
        if (position.getAvailableQuantity() < quantity) {
            throw new InsufficientHoldingsException("Insufficient holdings in " + t + ": requested " + quantity
                    + ", available " + position.getAvailableQuantity() + " shares");
        }
        if (reservationRepository.findByOrderId(orderId).isPresent()) {
            throw new BadRequestException("Order " + orderId + " already has a reservation");
        }
        position.setReservedQuantity(position.getReservedQuantity() + quantity);
        positionRepository.save(position);
        reservationRepository.save(CashReservation.builder()
                .portfolio(portfolio).orderId(orderId).ticker(t).side(OrderSide.SELL)
                .reservedQuantity(quantity).build());
        log.info("Reserved {} shares of {} for SELL order {}", quantity, t, orderId);
    }

    @Override
    @Transactional
    public void releaseReservation(String orderId) {
        CashReservation r = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found for order " + orderId));
        if (r.getStatus() != ReservationStatus.ACTIVE) {
            return;
        }
        Portfolio portfolio = r.getPortfolio();
        if (r.getSide() == OrderSide.BUY) {
            portfolio.setReservedCash(portfolio.getReservedCash().subtract(r.getReservedAmount()).max(BigDecimal.ZERO));
            portfolioRepository.save(portfolio);
            r.setReservedAmount(MoneyUtils.money(BigDecimal.ZERO));
        } else {
            positionRepository.findByPortfolioAndTickerAndStatus(portfolio, r.getTicker(), PositionStatus.OPEN)
                    .ifPresent(pos -> {
                        pos.setReservedQuantity(Math.max(0, pos.getReservedQuantity() - r.getReservedQuantity()));
                        positionRepository.save(pos);
                    });
            r.setReservedQuantity(0);
        }
        r.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(r);
        log.info("Released reservation of order {}", orderId);
    }

    @Override
    @Transactional
    public void applyExecution(TradeExecution e) {
        if (e.side() == null) {
            throw new BadRequestException("Execution side is required");
        }
        if (e.quantity() <= 0) {
            throw new BadRequestException("Execution quantity must be positive");
        }
        if (e.price() == null || e.price().signum() <= 0) {
            throw new BadRequestException("Execution price must be positive");
        }
        if (e.executionId() != null && transactionRepository.existsByExecutionId(e.executionId())) {
            throw new BadRequestException("execution already applied");
        }
        String t = normalize(e.ticker());
        Portfolio portfolio = provisioner.getOrCreate(e.userId());
        BigDecimal price = MoneyUtils.money(e.price());
        BigDecimal gross = MoneyUtils.money(price.multiply(BigDecimal.valueOf(e.quantity())));
        BigDecimal fee = feePolicy.feeFor(gross);
        Instant at = e.executedAt() != null ? e.executedAt() : Instant.now(clock);
        CashReservation reservation = e.orderId() == null ? null
                : reservationRepository.findByOrderId(e.orderId()).orElse(null);
        if (reservation != null && (!reservation.getPortfolio().getId().equals(portfolio.getId())
                || !reservation.getTicker().equals(t) || reservation.getSide() != e.side())) {
            throw new BadRequestException("Order " + e.orderId() + " belongs to another user, ticker or side");
        }

        if (e.side() == OrderSide.BUY) {
            applyBuy(portfolio, reservation, t, e, price, gross, fee, at);
        } else {
            applySell(portfolio, reservation, t, e, price, gross, fee, at);
        }
        portfolioRepository.save(portfolio);
        publisher.publishEvent(new PortfolioChangedEvent(e.userId(), portfolio.getId(), "EXECUTION"));
        log.info("Execution applied: user={} {} {} {} @ {} fee={}", e.userId(), e.side(), e.quantity(), t, price, fee);
    }

    private void applyBuy(Portfolio portfolio, CashReservation reservation, String t, TradeExecution e,
                          BigDecimal price, BigDecimal gross, BigDecimal fee, Instant at) {
        BigDecimal cost = MoneyUtils.money(gross.add(fee));
        if (reservation != null && reservation.getStatus() == ReservationStatus.ACTIVE
                && reservation.getSide() == OrderSide.BUY) {
            BigDecimal release = reservation.getReservedAmount().min(cost);
            reservation.setReservedAmount(reservation.getReservedAmount().subtract(release));
            portfolio.setReservedCash(portfolio.getReservedCash().subtract(release).max(BigDecimal.ZERO));
            if (reservation.getReservedAmount().signum() == 0) {
                reservation.setStatus(ReservationStatus.CONSUMED);
            }
            reservationRepository.save(reservation);
        }
        if (cost.compareTo(portfolio.getAvailableCash()) > 0) {
            throw new InsufficientFundsException("Insufficient funds: required " + cost
                    + " TND, available " + portfolio.getAvailableCash() + " TND");
        }
        Position position = positionRepository.findByPortfolioAndTickerAndStatus(portfolio, t, PositionStatus.OPEN)
                .orElseGet(() -> Position.builder().portfolio(portfolio).ticker(t).quantity(0)
                        .avgCost(MoneyUtils.cost(BigDecimal.ZERO)).openedAt(at).build());
        PnlCalculator.BuyResult result = calc.applyBuy(position.getQuantity(), position.getAvgCost(),
                e.quantity(), price, fee);
        position.setQuantity(result.newQuantity());
        position.setAvgCost(result.newAvgCost());
        position.setTotalCost(MoneyUtils.money(result.newAvgCost().multiply(BigDecimal.valueOf(result.newQuantity()))));
        positionRepository.save(position);

        portfolio.setCashBalance(portfolio.getCashBalance().subtract(result.cashOut()));
        portfolio.setTotalFees(portfolio.getTotalFees().add(fee));
        transactionRepository.save(ledger(portfolio, TransactionType.BUY, t, e, price, gross, fee,
                result.cashOut().negate(), null, at));
    }

    private void applySell(Portfolio portfolio, CashReservation reservation, String t, TradeExecution e,
                           BigDecimal price, BigDecimal gross, BigDecimal fee, Instant at) {
        Position position = positionRepository.findByPortfolioAndTickerAndStatus(portfolio, t, PositionStatus.OPEN)
                .orElseThrow(() -> new InsufficientHoldingsException("No open position in " + t));
        if (reservation != null && reservation.getStatus() == ReservationStatus.ACTIVE
                && reservation.getSide() == OrderSide.SELL) {
            long consume = Math.min(reservation.getReservedQuantity(), e.quantity());
            reservation.setReservedQuantity(reservation.getReservedQuantity() - consume);
            position.setReservedQuantity(Math.max(0, position.getReservedQuantity() - consume));
            if (reservation.getReservedQuantity() == 0) {
                reservation.setStatus(ReservationStatus.CONSUMED);
            }
            reservationRepository.save(reservation);
        }
        if (e.quantity() > position.getAvailableQuantity()) {
            throw new InsufficientHoldingsException("Insufficient holdings in " + t + ": requested "
                    + e.quantity() + ", available " + position.getAvailableQuantity() + " shares");
        }
        PnlCalculator.SellResult result = calc.applySell(position.getQuantity(), position.getAvgCost(),
                e.quantity(), price, fee);
        position.setQuantity(result.newQuantity());
        position.setRealizedPnl(position.getRealizedPnl().add(result.realizedPnl()));
        position.setTotalCost(MoneyUtils.money(position.getAvgCost().multiply(BigDecimal.valueOf(result.newQuantity()))));
        if (result.newQuantity() == 0) {
            position.setStatus(PositionStatus.CLOSED);
            position.setClosedAt(at);
            position.setReservedQuantity(0);
        }
        positionRepository.save(position);

        portfolio.setCashBalance(portfolio.getCashBalance().add(result.cashIn()));
        portfolio.setRealizedPnl(portfolio.getRealizedPnl().add(result.realizedPnl()));
        portfolio.setTotalFees(portfolio.getTotalFees().add(fee));
        transactionRepository.save(ledger(portfolio, TransactionType.SELL, t, e, price, gross, fee,
                result.cashIn(), result.realizedPnl(), at));
    }

    private PortfolioTransaction ledger(Portfolio portfolio, TransactionType type, String ticker, TradeExecution e,
                                        BigDecimal price, BigDecimal gross, BigDecimal fee, BigDecimal net,
                                        BigDecimal realized, Instant at) {
        return PortfolioTransaction.builder()
                .portfolio(portfolio).type(type).ticker(ticker).quantity(e.quantity()).price(price)
                .grossAmount(gross).fee(fee).netAmount(net).realizedPnl(realized)
                .cashBalanceAfter(portfolio.getCashBalance())
                .orderId(e.orderId()).executionId(e.executionId()).executedAt(at).build();
    }

    @Override
    @Transactional
    public BuyingPower getBuyingPower(Long userId) {
        Portfolio p = provisioner.getOrCreate(userId);
        Map<String, Long> shares = new HashMap<>();
        for (Position pos : positionRepository.findByPortfolioAndStatusOrderByTickerAsc(p, PositionStatus.OPEN)) {
            shares.put(pos.getTicker(), pos.getAvailableQuantity());
        }
        return new BuyingPower(userId, p.getCashBalance(), p.getReservedCash(), p.getAvailableCash(), shares);
    }

    private static void requireOrderAndQuantity(String orderId, long quantity) {
        if (orderId == null || orderId.isBlank()) {
            throw new BadRequestException("orderId is required");
        }
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }
    }

    private static String normalize(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new BadRequestException("Ticker is required");
        }
        return ticker.trim().toUpperCase(Locale.ROOT);
    }
}
