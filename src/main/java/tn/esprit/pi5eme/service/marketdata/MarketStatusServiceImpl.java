package tn.esprit.pi5eme.service.marketdata;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.pi5eme.config.MarketDataProperties;
import tn.esprit.pi5eme.dto.marketdata.MarketStatusResponse;
import tn.esprit.pi5eme.provider.MarketDataProvider;
import tn.esprit.pi5eme.repository.marketdata.InstrumentRepository;
import tn.esprit.pi5eme.scheduler.MarketCalendar;

@Service
@RequiredArgsConstructor
public class MarketStatusServiceImpl implements IMarketStatusService {

    private final MarketCalendar calendar;
    private final MarketDataProvider provider;
    private final IMarketDataSyncService syncService;
    private final MarketDataProperties props;
    private final InstrumentRepository instrumentRepository;

    @Override
    public MarketStatusResponse getStatus() {
        return MarketStatusResponse.builder()
                .open(calendar.isOpenNow())
                .provider(provider.name())
                .lastSyncAt(syncService.getLastSyncAt())
                .sessionOpen(props.getSessionOpen())
                .sessionClose(props.getSessionClose())
                .zone(MarketCalendar.ZONE.getId())
                .instrumentsCount(instrumentRepository.count())
                .build();
    }
}
