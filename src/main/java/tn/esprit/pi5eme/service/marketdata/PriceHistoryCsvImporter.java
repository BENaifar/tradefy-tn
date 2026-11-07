package tn.esprit.pi5eme.service.marketdata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.pi5eme.dto.marketdata.CsvImportResult;
import tn.esprit.pi5eme.entity.marketdata.Instrument;
import tn.esprit.pi5eme.entity.marketdata.PriceBar;
import tn.esprit.pi5eme.exception.BadRequestException;
import tn.esprit.pi5eme.repository.marketdata.InstrumentRepository;
import tn.esprit.pi5eme.repository.marketdata.PriceBarRepository;
import tn.esprit.pi5eme.util.MoneyUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Format: header row "date,ticker,open,high,low,close,volume"; separator "," or ";". */
@Component
@RequiredArgsConstructor
@Slf4j
public class PriceHistoryCsvImporter {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final List<String> COLUMNS = List.of("date", "ticker", "open", "high", "low", "close", "volume");

    private final InstrumentRepository instrumentRepository;
    private final PriceBarRepository priceBarRepository;

    @Transactional
    public CsvImportResult importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("File is larger than 5 MB");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        boolean textual = type.startsWith("text/") || type.contains("csv") || type.equals("application/vnd.ms-excel");
        if (!textual && !name.endsWith(".csv")) {
            throw new BadRequestException("Only CSV or text files are accepted");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return parse(reader);
        } catch (IOException e) {
            throw new BadRequestException("Could not read file: " + e.getMessage());
        }
    }

    private CsvImportResult parse(BufferedReader reader) throws IOException {
        CsvImportResult result = new CsvImportResult();
        String header = reader.readLine();
        int lineNo = 1;
        while (header != null && header.isBlank()) {
            header = reader.readLine();
            lineNo++;
        }
        if (header == null) {
            throw new BadRequestException("Missing header row");
        }
        if (header.startsWith("﻿")) {
            header = header.substring(1);
        }
        String sep = header.contains(";") ? ";" : ",";
        String[] names = header.split(sep, -1);
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < names.length; i++) {
            idx.put(names[i].trim().toLowerCase(Locale.ROOT), i);
        }
        for (String c : COLUMNS) {
            if (!idx.containsKey(c)) {
                throw new BadRequestException("Missing header column: " + c);
            }
        }

        Map<String, Optional<Instrument>> cache = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            lineNo++;
            if (line.isBlank()) {
                continue;
            }
            result.setTotalRows(result.getTotalRows() + 1);
            String[] cols = line.split(sep, -1);
            try {
                if (cols.length < names.length) {
                    throw new IllegalArgumentException("expected " + names.length + " columns");
                }
                String ticker = cols[idx.get("ticker")].trim().toUpperCase(Locale.ROOT);
                Optional<Instrument> inst = cache.computeIfAbsent(ticker,
                        t -> instrumentRepository.findByTickerIgnoreCase(t));
                if (inst.isEmpty()) {
                    result.getErrors().add("line " + lineNo + ": unknown ticker " + ticker);
                    continue;
                }
                LocalDate date = parseDate(cols[idx.get("date")].trim());
                BigDecimal open = number(cols[idx.get("open")], sep);
                BigDecimal high = number(cols[idx.get("high")], sep);
                BigDecimal low = number(cols[idx.get("low")], sep);
                BigDecimal close = number(cols[idx.get("close")], sep);
                long volume = Long.parseLong(cols[idx.get("volume")].trim());

                Optional<PriceBar> existing = priceBarRepository.findByInstrumentAndDate(inst.get(), date);
                PriceBar bar = existing.orElseGet(() -> PriceBar.builder().instrument(inst.get()).date(date).build());
                bar.setOpen(open);
                bar.setHigh(high);
                bar.setLow(low);
                bar.setClose(close);
                bar.setVolume(volume);
                priceBarRepository.save(bar);
                if (existing.isPresent()) {
                    result.setUpdated(result.getUpdated() + 1);
                } else {
                    result.setInserted(result.getInserted() + 1);
                }
            } catch (RuntimeException e) {
                result.getErrors().add("line " + lineNo + ": " + e.getMessage());
            }
        }
        log.info("History CSV import: {} rows, {} inserted, {} updated, {} errors",
                result.getTotalRows(), result.getInserted(), result.getUpdated(), result.getErrors().size());
        return result;
    }

    private static LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("invalid date " + s);
        }
    }

    private static BigDecimal number(String s, String sep) {
        String v = s.trim();
        if (";".equals(sep)) {
            v = v.replace(",", ".");
        }
        try {
            return MoneyUtils.money(new BigDecimal(v));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid number " + s.trim());
        }
    }
}
