package tn.esprit.pi5eme.controller.marketdata;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.pi5eme.config.SectorSeeder;
import tn.esprit.pi5eme.dto.ApiResponse;
import tn.esprit.pi5eme.dto.marketdata.CsvImportResult;
import tn.esprit.pi5eme.service.marketdata.IMarketDataSyncService;
import tn.esprit.pi5eme.service.marketdata.IPriceHistoryService;

@RestController
@RequestMapping("/api/market/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MarketAdminController {

    private final IMarketDataSyncService syncService;
    private final IPriceHistoryService historyService;
    private final SectorSeeder sectorSeeder;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<IMarketDataSyncService.SyncResult>> sync() {
        IMarketDataSyncService.SyncResult result = syncService.refresh();
        sectorSeeder.seed();
        return ResponseEntity.ok(ApiResponse.success("Market data synchronized", result));
    }

    @PostMapping("/close-session")
    public ResponseEntity<ApiResponse<Void>> closeSession() {
        syncService.closeSession();
        return ResponseEntity.ok(ApiResponse.success("Session closed"));
    }

    @PostMapping(value = "/history/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CsvImportResult>> importHistory(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("History imported", historyService.importCsv(file)));
    }
}
