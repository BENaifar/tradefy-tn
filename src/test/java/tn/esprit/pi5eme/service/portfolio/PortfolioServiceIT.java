package tn.esprit.pi5eme.service.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.dto.PageResponse;
import tn.esprit.pi5eme.dto.marketdata.InstrumentResponse;
import tn.esprit.pi5eme.dto.marketdata.QuoteResponse;
import tn.esprit.pi5eme.dto.portfolio.CashOperationRequest;
import tn.esprit.pi5eme.dto.portfolio.PortfolioSummaryResponse;
import tn.esprit.pi5eme.dto.portfolio.TradeExecution;
import tn.esprit.pi5eme.dto.portfolio.TransactionResponse;
import tn.esprit.pi5eme.entity.portfolio.OrderSide;
import tn.esprit.pi5eme.entity.portfolio.TransactionType;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.exception.InsufficientFundsException;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.service.marketdata.IMarketDataService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class PortfolioServiceIT {

    private static final long USER = 11L;

    @MockBean IMarketDataService marketData;
    @MockBean MarketDataProvider provider;
    @Autowired IPortfolioService service;
    @Autowired IPortfolioTradingGateway gateway;

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    @BeforeEach
    void mocks() {
        when(marketData.getInstrument(anyString())).thenAnswer(inv ->
                InstrumentResponse.builder().ticker(inv.getArgument(0)).build());
        when(marketData.getLastPrice("AAA")).thenReturn(d("11.000"));
        when(marketData.getLastPrices(anyCollection())).thenReturn(Map.of("AAA", d("11.000")));
        when(marketData.getQuotes(anyCollection())).thenReturn(Map.of("AAA",
                QuoteResponse.builder().ticker("AAA").last(d("11.000")).previousClose(d("10.000")).build()));
    }

    private CashOperationRequest cash(String amount) {
        return CashOperationRequest.builder().amount(d(amount)).build();
    }

    private void buy(long qty, String price) {
        gateway.applyExecution(new TradeExecution(USER, null, null, "AAA", OrderSide.BUY, qty, d(price), Instant.now()));
    }

    @Test
    void firstSummaryCreatesPortfolio() {
        PortfolioSummaryResponse s = service.getSummary(USER);
        assertEquals(d("100000.000"), s.getCashBalance());
        assertEquals(d("100000.000"), s.getTotalValue());
        assertEquals(0, s.getTotalPnl().signum());
    }

    @Test
    void depositAddsCashAndContributions() {
        service.deposit(USER, cash("500"));
        PortfolioSummaryResponse s = service.getSummary(USER);
        assertEquals(d("100500.000"), s.getCashBalance());
        assertEquals(d("100500.000"), s.getNetContributions());
    }

    @Test
    void depositAboveMaxRejected() {
        assertThrows(BadRequestException.class, () -> service.deposit(USER, cash("2000000")));
    }

    @Test
    void withdrawMoreThanAvailableRejected() {
        assertThrows(InsufficientFundsException.class, () -> service.withdraw(USER, cash("100000.001")));
    }

    @Test
    void withdrawReducesContributions() {
        service.withdraw(USER, cash("1000"));
        PortfolioSummaryResponse s = service.getSummary(USER);
        assertEquals(d("99000.000"), s.getCashBalance());
        assertEquals(d("99000.000"), s.getNetContributions());
    }

    @Test
    void resetBlockedByActiveReservation() {
        gateway.reserveForBuy(USER, "o1", "AAA", 10, d("11.000"));
        assertThrows(BadRequestException.class, () -> service.reset(USER));
    }

    @Test
    void resetRestoresInitialState() {
        buy(100, "10.000");
        PortfolioSummaryResponse s = service.reset(USER);
        assertEquals(d("100000.000"), s.getCashBalance());
        assertEquals(0, s.getOpenPositionsCount());
        assertEquals(0, s.getRealizedPnl().signum());
        PageResponse<TransactionResponse> page = service.getTransactions(USER, TransactionType.RESET, null, null, null, 0, 10);
        assertEquals(1, page.getTotalElements());
    }

    @Autowired ISnapshotService snapshotService;
    @Autowired tn.esprit.pi5eme.repository.portfolio.PortfolioSnapshotRepository snapshots;
    @Autowired tn.esprit.pi5eme.repository.portfolio.PortfolioRepository portfolios;
    @Autowired tn.esprit.pi5eme.scheduler.MarketCalendar calendar;

    @Test
    void resetDropsSnapshotTakenEarlierToday() {
        buy(100, "10.000");
        var p = portfolios.findByUserId(USER).orElseThrow();
        snapshotService.takeSnapshot(p, calendar.today());

        service.reset(USER);

        assertTrue(snapshots.findByPortfolioAndDate(p, calendar.today()).isEmpty());
    }

    @Test
    void dayPnl() {
        buy(100, "10.000");
        PortfolioSummaryResponse s = service.getSummary(USER);
        assertEquals(d("100.000"), s.getDayPnl());
        assertEquals(1, s.getOpenPositionsCount());
        // total P&L = totalValue - netContributions
        assertEquals(s.getTotalValue().subtract(s.getNetContributions()), s.getTotalPnl());
    }

    @Test
    void transactionFilterAndSizeClamp() {
        buy(10, "10.000");
        service.deposit(USER, cash("5"));
        PageResponse<TransactionResponse> buys = service.getTransactions(USER, TransactionType.BUY, null, null, null, 0, 1000);
        assertEquals(1, buys.getTotalElements());
        assertEquals(100, buys.getSize());
        assertEquals(TransactionType.BUY, buys.getContent().get(0).getType());
        assertEquals(3, service.getTransactions(USER, null, null, null, null, 0, 20).getTotalElements());
    }
}
