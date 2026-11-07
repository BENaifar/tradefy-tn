package tn.esprit.pi5eme.service.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.marketdata.InstrumentResponse;
import tn.esprit.pi5eme.dto.portfolio.BuyingPower;
import tn.esprit.pi5eme.dto.portfolio.TradeExecution;
import tn.esprit.pi5eme.entity.portfolio.*;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.exception.InsufficientFundsException;
import tn.esprit.pi5eme.exception.InsufficientHoldingsException;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.repository.portfolio.*;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class PortfolioTradingGatewayIT {

    private static final long USER = 1L;

    @MockBean IMarketDataService marketData;
    @MockBean MarketDataProvider provider;
    @Autowired IPortfolioTradingGateway gateway;
    @Autowired PortfolioRepository portfolios;
    @Autowired PositionRepository positions;
    @Autowired PortfolioTransactionRepository transactions;
    @Autowired CashReservationRepository reservations;

    @BeforeEach
    void mocks() {
        when(marketData.getInstrument(anyString())).thenAnswer(inv ->
                InstrumentResponse.builder().ticker(inv.getArgument(0)).build());
        when(marketData.getLastPrice("AAA")).thenReturn(new BigDecimal("10.000"));
    }

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    private TradeExecution exec(String orderId, String execId, OrderSide side, long qty, String price) {
        return new TradeExecution(USER, orderId, execId, "AAA", side, qty, d(price), Instant.now());
    }

    private Portfolio portfolio() {
        return portfolios.findByUserId(USER).orElseThrow();
    }

    private Position openPosition() {
        return positions.findByPortfolioAndTickerAndStatus(portfolio(), "AAA", PositionStatus.OPEN).orElseThrow();
    }

    @Test
    void referenceScenario() {
        gateway.applyExecution(exec(null, null, OrderSide.BUY, 100, "10.000"));
        assertEquals(d("98996.000"), portfolio().getCashBalance());
        assertEquals(d("10.040000"), openPosition().getAvgCost());

        gateway.applyExecution(exec(null, null, OrderSide.BUY, 50, "12.000"));
        assertEquals(d("98393.600"), portfolio().getCashBalance());
        assertEquals(d("10.709333"), openPosition().getAvgCost());

        gateway.applyExecution(exec(null, null, OrderSide.SELL, 60, "13.000"));
        assertEquals(d("99170.480"), portfolio().getCashBalance());
        assertEquals(90, openPosition().getQuantity());
        assertEquals(d("134.320"), openPosition().getRealizedPnl());

        gateway.applyExecution(exec(null, null, OrderSide.SELL, 90, "9.000"));
        Portfolio p = portfolio();
        assertEquals(d("99977.240"), p.getCashBalance());
        assertEquals(d("-22.760"), p.getRealizedPnl());
        assertEquals(d("12.760"), p.getTotalFees());
        assertTrue(positions.findByPortfolioAndTickerAndStatus(p, "AAA", PositionStatus.OPEN).isEmpty());
        assertEquals(1, positions.findByPortfolioAndStatusOrderByClosedAtDesc(p, PositionStatus.CLOSED).size());

        List<PortfolioTransaction> ledger = transactions.findAll();
        assertEquals(5, ledger.size());
        assertEquals(1, ledger.stream().filter(t -> t.getType() == TransactionType.INITIAL_DEPOSIT).count());
        assertEquals(4, ledger.stream().filter(t -> t.getType() == TransactionType.BUY
                || t.getType() == TransactionType.SELL).count());
    }

    @Test
    void reserveThenFillEverything() {
        gateway.reserveForBuy(USER, "o1", "AAA", 100, d("10.000"));
        assertEquals(d("1004.000"), portfolio().getReservedCash());
        assertEquals(d("98996.000"), portfolio().getAvailableCash());

        gateway.applyExecution(exec("o1", "e1", OrderSide.BUY, 100, "10.000"));
        assertEquals(0, portfolio().getReservedCash().signum());
        assertEquals(d("98996.000"), portfolio().getCashBalance());
        assertEquals(ReservationStatus.CONSUMED, reservations.findByOrderId("o1").orElseThrow().getStatus());
    }

    @Test
    void partialFillThenRelease() {
        gateway.reserveForBuy(USER, "o1", "AAA", 100, d("10.000"));
        gateway.applyExecution(exec("o1", "e1", OrderSide.BUY, 40, "10.000"));
        assertEquals(d("602.400"), reservations.findByOrderId("o1").orElseThrow().getReservedAmount());

        gateway.releaseReservation("o1");
        assertEquals(0, portfolio().getReservedCash().signum());
        assertEquals(ReservationStatus.RELEASED, reservations.findByOrderId("o1").orElseThrow().getStatus());
        assertEquals(d("99598.400"), portfolio().getCashBalance());
    }

    @Test
    void priceImprovement() {
        gateway.reserveForBuy(USER, "o1", "AAA", 100, d("10.000"));
        gateway.applyExecution(exec("o1", "e1", OrderSide.BUY, 100, "9.500"));
        CashReservation r = reservations.findByOrderId("o1").orElseThrow();
        assertEquals(d("50.200"), r.getReservedAmount());
        assertEquals(ReservationStatus.ACTIVE, r.getStatus());

        gateway.releaseReservation("o1");
        assertEquals(0, portfolio().getReservedCash().signum());
    }

    @Test
    void marketOrderReserveUsesBuffer() {
        gateway.reserveForBuy(USER, "o1", "AAA", 100, null);
        assertEquals(d("1054.200"), portfolio().getReservedCash());
    }

    @Test
    void insufficientFundsChangesNothing() {
        gateway.getBuyingPower(USER);
        assertThrows(InsufficientFundsException.class,
                () -> gateway.reserveForBuy(USER, "o1", "AAA", 20000, d("10.000")));
        assertEquals(0, portfolio().getReservedCash().signum());
        assertTrue(reservations.findByOrderId("o1").isEmpty());
    }

    @Test
    void insufficientHoldings() {
        assertThrows(InsufficientHoldingsException.class, () -> gateway.reserveForSell(USER, "s0", "AAA", 10));

        gateway.applyExecution(exec(null, null, OrderSide.BUY, 100, "10.000"));
        gateway.reserveForSell(USER, "s1", "AAA", 60);
        assertThrows(InsufficientHoldingsException.class, () -> gateway.reserveForSell(USER, "s2", "AAA", 50));
    }

    @Test
    void sellWithReservation() {
        gateway.applyExecution(exec(null, null, OrderSide.BUY, 100, "10.000"));
        gateway.reserveForSell(USER, "s1", "AAA", 60);
        gateway.applyExecution(exec("s1", "e2", OrderSide.SELL, 60, "11.000"));
        Position p = openPosition();
        assertEquals(40, p.getQuantity());
        assertEquals(0, p.getReservedQuantity());
    }

    @Test
    void duplicateExecutionIdRejected() {
        gateway.applyExecution(exec(null, "dup", OrderSide.BUY, 10, "10.000"));
        assertThrows(BadRequestException.class,
                () -> gateway.applyExecution(exec(null, "dup", OrderSide.BUY, 10, "10.000")));
    }

    @Test
    void rebuyAfterCloseCreatesNewPosition() {
        gateway.applyExecution(exec(null, null, OrderSide.BUY, 10, "10.000"));
        gateway.applyExecution(exec(null, null, OrderSide.SELL, 10, "11.000"));
        gateway.applyExecution(exec(null, null, OrderSide.BUY, 5, "10.000"));
        Portfolio p = portfolio();
        assertEquals(1, positions.findByPortfolioAndStatusOrderByTickerAsc(p, PositionStatus.OPEN).size());
        assertEquals(1, positions.findByPortfolioAndStatusOrderByClosedAtDesc(p, PositionStatus.CLOSED).size());
    }

    @Test
    void buyingPowerReflectsReservations() {
        gateway.applyExecution(exec(null, null, OrderSide.BUY, 100, "10.000"));
        gateway.reserveForSell(USER, "s1", "AAA", 30);
        gateway.reserveForBuy(USER, "b1", "AAA", 10, d("10.000"));
        BuyingPower bp = gateway.getBuyingPower(USER);
        assertEquals(70L, bp.availableShares().get("AAA"));
        assertEquals(d("100.400"), bp.reservedCash());
        assertEquals(bp.cashBalance().subtract(bp.reservedCash()), bp.availableCash());
    }

    @Test
    void executionCannotConsumeAnotherUsersReservation() {
        gateway.reserveForBuy(USER, "o-user1", "AAA", 100, d("10.000"));
        TradeExecution foreign = new TradeExecution(2L, "o-user1", "x1", "AAA", OrderSide.BUY, 10, d("10.000"), Instant.now());

        assertThrows(BadRequestException.class, () -> gateway.applyExecution(foreign));
        assertEquals(d("1004.000"), portfolio().getReservedCash());
    }
}
