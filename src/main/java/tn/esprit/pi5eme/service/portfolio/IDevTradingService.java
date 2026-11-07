package tn.esprit.pi5eme.service.portfolio;

import tn.esprit.pi5eme.dto.portfolio.DevExecutionRequest;
import tn.esprit.pi5eme.dto.portfolio.PositionResponse;

/** Temporary. Delete when the trading module calls IPortfolioTradingGateway. */
public interface IDevTradingService {

    PositionResponse execute(Long userId, DevExecutionRequest request);
}
