package com.smarttransit.smart_transit.journey;

import java.math.BigDecimal;

/**
 * The traveller's optional limits on a search. Either part may be null, meaning "no limit". The budget is a fare
 * per traveller, judged the same way the result shows it: the cheapest class that still has room for the party.
 */
public record JourneyFilter(DepartureWindow departureWindow, BigDecimal maxFarePerTraveller) {

	public static final JourneyFilter NONE = new JourneyFilter(null, null);

	public boolean isActive() {
		return departureWindow != null || maxFarePerTraveller != null;
	}

	public boolean accepts(Itinerary itinerary, int passengers) {
		if (departureWindow != null && !departureWindow.contains(itinerary.departure().toLocalTime())) {
			return false;
		}
		return maxFarePerTraveller == null
				|| itinerary.farePerPassenger(passengers).compareTo(maxFarePerTraveller) <= 0;
	}
}
