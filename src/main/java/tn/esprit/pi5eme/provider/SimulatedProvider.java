package tn.esprit.pi5eme.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.config.MarketDataProperties;
import tn.esprit.pi5eme.dto.marketdata.RawQuote;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.Quote;
import tn.esprit.pi5eme.repository.marketdata.QuoteRepository;
import tn.esprit.pi5eme.scheduler.MarketCalendar;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Random-walk prices for demos outside market hours. */
@Component
@ConditionalOnProperty(name = "app.marketdata.provider", havingValue = "simulated")
@Slf4j
public class SimulatedProvider implements MarketDataProvider {

    private static final BigDecimal MIN_PRICE = new BigDecimal("0.001");

    private final QuoteRepository quoteRepository;
    private final MarketDataProperties props;
    private final BvmtQuoteMapper mapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Random random;

    private final Map<String, RawQuote> current = new LinkedHashMap<>();
    private boolean bootstrapped;

    public SimulatedProvider(QuoteRepository quoteRepository, MarketDataProperties props, BvmtQuoteMapper mapper,
                             ObjectMapper objectMapper, Clock clock) {
        this.quoteRepository = quoteRepository;
        this.props = props;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.random = props.getSimulatedSeed() != null ? new Random(props.getSimulatedSeed()) : new Random();
    }

    @Override
    public synchronized List<RawQuote> fetchSnapshot() {
        if (!bootstrapped) {
            bootstrap();
            bootstrapped = true;
        }
        LocalDate today = LocalDate.now(clock.withZone(MarketCalendar.ZONE));
        LocalTime now = LocalTime.now(clock.withZone(MarketCalendar.ZONE)).withNano(0);
        List<RawQuote> result = new ArrayList<>();
        for (Map.Entry<String, RawQuote> e : current.entrySet()) {
            RawQuote next = step(e.getValue(), today, now);
            e.setValue(next);
            result.add(next);
        }
        return result;
    }

    @Override
    public String name() {
        return "SIMULATED";
    }

    private RawQuote step(RawQuote q, LocalDate today, LocalTime now) {
        BigDecimal r = BigDecimal.valueOf(random.nextGaussian()).multiply(props.getSimulatedVolatility());
        BigDecimal newLast = q.last().multiply(BigDecimal.ONE.add(r));
        if (q.upperLimit() != null && newLast.compareTo(q.upperLimit()) > 0) {
            newLast = q.upperLimit();
        }
        if (q.lowerLimit() != null && newLast.compareTo(q.lowerLimit()) < 0) {
            newLast = q.lowerLimit();
        }
        newLast = MoneyUtils.money(newLast);
        if (newLast.compareTo(MIN_PRICE) < 0) {
            newLast = MIN_PRICE;
        }
        long deltaVolume = random.nextInt(501);
        long volume = q.volume() + deltaVolume;
        BigDecimal turnover = MoneyUtils.money(MoneyUtils.nz(q.turnover())
                .add(newLast.multiply(BigDecimal.valueOf(deltaVolume))));
        BigDecimal high = q.high() == null ? newLast : q.high().max(newLast);
        BigDecimal low = q.low() == null ? newLast : q.low().min(newLast);
        BigDecimal change = null;
        if (q.previousClose() != null && q.previousClose().signum() > 0) {
            change = MoneyUtils.percent(newLast.subtract(q.previousClose())
                    .divide(q.previousClose(), 10, MoneyUtils.RM).multiply(MoneyUtils.HUNDRED));
        }
        return new RawQuote(q.ticker(), q.isin(), q.name(), q.arabName(), q.valGroup(),
                newLast, q.previousClose(), q.open(), MoneyUtils.money(high), MoneyUtils.money(low),
                change, volume, turnover, q.bid(), q.ask(), q.bidQty(), q.askQty(),
                q.upperLimit(), q.lowerLimit(), today, now);
    }

    private void bootstrap() {
        List<Quote> stored = quoteRepository.findAllActiveWithInstrument();
        if (!stored.isEmpty()) {
            for (Quote q : stored) {
                if (q.getLast() == null) {
                    continue;
                }
                Instrument i = q.getInstrument();
                current.put(i.getTicker(), new RawQuote(i.getTicker(), i.getIsin(), i.getName(), i.getArabName(),
                        i.getValGroup(), q.getLast(), q.getPreviousClose(), q.getOpen(), q.getHigh(), q.getLow(),
                        q.getChangePercent(), q.getVolume(), q.getTurnover(), q.getBid(), q.getAsk(),
                        q.getBidQty(), q.getAskQty(), q.getUpperLimit(), q.getLowerLimit(),
                        q.getSessionDate(), q.getQuoteTime()));
            }
            log.info("Simulated provider bootstrapped from {} stored quotes", current.size());
            return;
        }
        ClassPathResource seed = new ClassPathResource("marketdata/bvmt-seed.json");
        if (seed.exists()) {
            try (InputStream in = seed.getInputStream()) {
                BvmtMarketResponse response = objectMapper.readValue(in, BvmtMarketResponse.class);
                for (RawQuote q : mapper.map(response, LocalDate.now(clock.withZone(MarketCalendar.ZONE)))) {
                    current.put(q.ticker(), q);
                }
                log.info("Simulated provider bootstrapped from seed file with {} quotes", current.size());
                return;
            } catch (IOException e) {
                log.warn("Could not read bvmt-seed.json: {}", e.getMessage());
            }
        }
        LocalDate today = LocalDate.now(clock.withZone(MarketCalendar.ZONE));
        for (int n = 1; n <= 5; n++) {
            String t = "SIM" + n;
            BigDecimal ten = new BigDecimal("10.000");
            current.put(t, new RawQuote(t, "TNSIM" + String.format("%07d", n), "Simulated " + n, null, "99",
                    ten, ten, null, ten, ten, BigDecimal.ZERO.setScale(4), 0, BigDecimal.ZERO.setScale(3),
                    null, null, 0, 0, null, null, today, LocalTime.of(9, 0)));
        }
        log.info("Simulated provider bootstrapped with 5 fake instruments");
    }
}
