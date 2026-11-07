package tn.esprit.pi5eme.provider;

import tn.esprit.pi5eme.dto.marketdata.RawQuote;

import java.util.List;

public interface MarketDataProvider {

    /** Throws MarketDataUnavailableException on failure. */
    List<RawQuote> fetchSnapshot();

    String name();
}
