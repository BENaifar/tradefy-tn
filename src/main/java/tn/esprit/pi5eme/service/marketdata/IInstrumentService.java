package tn.esprit.pi5eme.service.marketdata;

import tn.esprit.pi5eme.dto.marketdata.InstrumentResponse;
import tn.esprit.pi5eme.dto.marketdata.InstrumentUpdateRequest;
import tn.esprit.pi5eme.dto.marketdata.MoverType;
import tn.esprit.pi5eme.dto.marketdata.QuoteResponse;

import java.util.List;

public interface IInstrumentService {

    /** Active instruments matching name/ticker text and/or sector, with their quotes attached. */
    List<InstrumentResponse> search(String q, String sector);

    InstrumentResponse getByTicker(String ticker);

    List<String> getSectors();

    /** Admin: null request fields are left unchanged. */
    InstrumentResponse update(String ticker, InstrumentUpdateRequest req);

    /** Top movers; limit is clamped to 1..50. */
    List<QuoteResponse> getMovers(MoverType type, int limit);

    /** All quotes of active instruments. */
    List<QuoteResponse> getAllQuotes();
}
