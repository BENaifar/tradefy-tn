package tn.esprit.pi5eme.dto.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Provider-neutral quote format. Every provider outputs this. */
public record RawQuote(String ticker, String isin, String name, String arabName, String valGroup,
                       BigDecimal last, BigDecimal previousClose, BigDecimal open, BigDecimal high, BigDecimal low,
                       BigDecimal changePercent, long volume, BigDecimal turnover,
                       BigDecimal bid, BigDecimal ask, long bidQty, long askQty,
                       BigDecimal upperLimit, BigDecimal lowerLimit,
                       LocalDate sessionDate, LocalTime quoteTime) {}
