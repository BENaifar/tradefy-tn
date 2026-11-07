package tn.esprit.pi5eme.service.marketdata;

import tn.esprit.pi5eme.dto.marketdata.WatchlistItemResponse;

import java.util.List;

public interface IWatchlistService {

    /** The user's watchlist, each item with its quote (null if no quote yet). */
    List<WatchlistItemResponse> list(Long userId);

    /** Unknown ticker gives 404; already present gives BadRequestException. */
    WatchlistItemResponse add(Long userId, String ticker);

    /** Not present gives 404. */
    void remove(Long userId, String ticker);
}
