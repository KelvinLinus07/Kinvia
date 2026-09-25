package com.smarttransit.smart_transit.recommendation;

/** Everything a scorer may consider besides the itineraries themselves; extend here for personalisation. */
public record RankingContext(RankingStrategy strategy, int passengers) {
}
