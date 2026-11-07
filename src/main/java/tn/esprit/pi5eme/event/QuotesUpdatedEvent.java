package tn.esprit.pi5eme.event;

import tn.esprit.pi5eme.dto.marketdata.QuoteResponse;

import java.time.Instant;
import java.util.List;

public record QuotesUpdatedEvent(List<QuoteResponse> quotes, Instant at) {}
