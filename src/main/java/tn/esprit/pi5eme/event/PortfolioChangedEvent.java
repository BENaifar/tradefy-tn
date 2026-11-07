package tn.esprit.pi5eme.event;

public record PortfolioChangedEvent(Long userId, Long portfolioId, String reason) {}
