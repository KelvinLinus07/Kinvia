package com.smarttransit.smart_transit.recommendation;

import java.util.List;

import com.smarttransit.smart_transit.journey.Itinerary;

/** Orders candidate itineraries best-first. Implementations may be rule-based, personalised or model-backed. */
public interface JourneyScorer {

	List<ScoredItinerary> rank(List<Itinerary> candidates, RankingContext context);
}
