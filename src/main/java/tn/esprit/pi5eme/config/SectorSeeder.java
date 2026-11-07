package tn.esprit.pi5eme.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.repository.marketdata.InstrumentRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Seeds instrument sectors from marketdata/sectors.csv. Only sets a sector where it is still null.
 * Called by MarketDataScheduler.syncOnStartup() after the first refresh.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SectorSeeder {

    private final InstrumentRepository instrumentRepository;

    @Transactional
    public int seed() {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("marketdata/sectors.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
                    continue;
                }
                Instrument i = instrumentRepository.findByTickerIgnoreCase(parts[0].trim()).orElse(null);
                if (i != null && i.getSector() == null) {
                    i.setSector(parts[1].trim());
                    instrumentRepository.save(i);
                    count++;
                }
            }
        } catch (IOException e) {
            log.warn("Could not read sectors.csv: {}", e.getMessage());
        }
        log.info("Sector seed: {} instruments updated", count);
        return count;
    }
}
