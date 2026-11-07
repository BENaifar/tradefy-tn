package tn.esprit.pi5eme.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.dto.marketdata.RawQuote;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Slf4j
public class BvmtQuoteMapper {

    private static final DateTimeFormatter SEANCE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);

    public List<RawQuote> map(BvmtMarketResponse response, LocalDate fallbackDate) {
        List<RawQuote> result = new ArrayList<>();
        if (response == null || response.getMarkets() == null) {
            return result;
        }
        for (BvmtMarketResponse.Item item : response.getMarkets()) {
            RawQuote q = map(item, fallbackDate);
            if (q != null) {
                result.add(q);
            }
        }
        return result;
    }

    RawQuote map(BvmtMarketResponse.Item item, LocalDate fallbackDate) {
        if (item == null || item.getReferentiel() == null) {
            log.warn("Skipping BVMT item without referentiel");
            return null;
        }
        BvmtMarketResponse.Referentiel ref = item.getReferentiel();
        if (ref.getTicker() == null || ref.getTicker().isBlank()) {
            log.warn("Skipping BVMT item with blank ticker");
            return null;
        }
        if (item.getLast() == null) {
            log.warn("Skipping BVMT item {} without last price", ref.getTicker());
            return null;
        }
        String ticker = ref.getTicker().trim().toUpperCase(Locale.ROOT);
        BigDecimal last = MoneyUtils.money(item.getLast());
        BigDecimal previousClose = MoneyUtils.money(item.getClose());
        BigDecimal changePercent = MoneyUtils.percent(item.getChange());
        if (changePercent == null && previousClose != null && previousClose.signum() > 0) {
            changePercent = MoneyUtils.percent(last.subtract(previousClose)
                    .divide(previousClose, 10, MoneyUtils.RM).multiply(MoneyUtils.HUNDRED));
        }
        BvmtMarketResponse.Limit limit = item.getLimit();
        String isin = ref.getIsin() != null && !ref.getIsin().isBlank() ? ref.getIsin() : item.getIsin();

        return new RawQuote(
                ticker, isin, ref.getStockName(), ref.getArabName(), ref.getValGroup(),
                last, previousClose,
                zeroToNull(item.getOpen()), zeroToNull(item.getHigh()), zeroToNull(item.getLow()),
                changePercent,
                item.getVolume() == null ? 0L : item.getVolume(),
                MoneyUtils.money(item.getCaps()),
                limit == null ? null : zeroToNull(limit.getBid()),
                limit == null ? null : zeroToNull(limit.getAsk()),
                limit == null || limit.getBidQty() == null ? 0L : limit.getBidQty(),
                limit == null || limit.getAskQty() == null ? 0L : limit.getAskQty(),
                zeroToNull(item.getMax()), zeroToNull(item.getMin()),
                parseDate(item.getSeance(), fallbackDate), parseTime(item.getTime()));
    }

    private static BigDecimal zeroToNull(BigDecimal v) {
        return v == null || v.signum() == 0 ? null : MoneyUtils.money(v);
    }

    private LocalDate parseDate(String s, LocalDate fallback) {
        try {
            return LocalDate.parse(s.trim(), SEANCE_FORMAT);
        } catch (DateTimeParseException | NullPointerException e) {
            log.warn("Unparseable BVMT seance date \"{}\", using fallback {}", s, fallback);
            return fallback;
        }
    }

    private static LocalTime parseTime(String s) {
        try {
            return LocalTime.parse(s.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }
}
