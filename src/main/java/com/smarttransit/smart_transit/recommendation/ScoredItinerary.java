package com.smarttransit.smart_transit.recommendation;

import java.util.Set;

import com.smarttransit.smart_transit.journey.Itinerary;

/** An itinerary with its 0-100 score (relative to the candidates it was ranked against) and highlight tags. */
public record ScoredItinerary(Itinerary itinerary, double score, Set<JourneyTag> tags) {
}
