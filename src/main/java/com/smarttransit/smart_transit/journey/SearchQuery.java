package com.smarttransit.smart_transit.journey;

import java.time.LocalDate;

import com.smarttransit.smart_transit.recommendation.RankingStrategy;

/**
 * A journey search. Null {@code transport}, {@code ranking} and {@code maxTransfers} fall back to the traveller's
 * preferences; {@code filter} limits the departure time and fare of the journeys returned.
 */
public record SearchQuery(long originId, long destinationId, LocalDate date, int passengers,
		TransportPreference transport, RankingStrategy ranking, Integer maxTransfers, JourneyFilter filter) {
}
