package tn.esprit.pi5eme.service.portfolio;

import tn.esprit.pi5eme.dto.portfolio.PositionDetailResponse;
import tn.esprit.pi5eme.dto.portfolio.PositionResponse;

import java.util.List;

public interface IPositionService {

    /** Open positions valued with live prices; weightPct is relative to the portfolio total value. */
    List<PositionResponse> getOpenPositions(Long userId);

    List<PositionResponse> getClosedPositions(Long userId);

    /** The OPEN position (or the latest CLOSED one) with its transactions; 404 if never held. */
    PositionDetailResponse getPosition(Long userId, String ticker);
}
