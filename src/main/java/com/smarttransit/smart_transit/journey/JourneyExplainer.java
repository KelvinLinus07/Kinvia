package com.smarttransit.smart_transit.journey;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.smarttransit.smart_transit.recommendation.JourneyTag;
import com.smarttransit.smart_transit.recommendation.ScoredItinerary;

/** Produces plain-language reasons for a result. Deterministic and derived only from the itinerary and its tags. */
final class JourneyExplainer {

	private static final int LOW_SEAT_THRESHOLD = 6;

	private JourneyExplainer() {
	}

	static List<String> highlights(ScoredItinerary scored, int passengers, int optionCount) {
		Itinerary itinerary = scored.itinerary();
		List<String> notes = new ArrayList<>();
		if (JourneyMapper.has(scored, JourneyTag.RECOMMENDED)) {
			notes.add("Best match for your preferences among " + optionCount + " options");
		}
		if (JourneyMapper.has(scored, JourneyTag.FASTEST)) {
			notes.add("Fastest option");
		}
		if (JourneyMapper.has(scored, JourneyTag.CHEAPEST)) {
			notes.add("Lowest fare");
		}
		if (JourneyMapper.has(scored, JourneyTag.MOST_COMFORTABLE)) {
			notes.add("Most comfortable seating");
		}
		if (itinerary.transfers() == 0) {
			notes.add("Direct, no changes");
		}
		else {
			String changes = java.util.stream.IntStream.range(1, itinerary.legs().size())
					.mapToObj(i -> "change at " + itinerary.legs().get(i).board().stationName() + " ("
							+ minutes(itinerary.waitBefore(i).toMinutes()) + " wait)")
					.collect(Collectors.joining(", "));
			notes.add(capitalise(changes));
		}
		if (itinerary.isMultimodal()) {
			notes.add("Combines " + itinerary.legs().stream().map(l -> JourneyMapper.modeLabel(l.trip().mode()))
					.distinct().collect(Collectors.joining(" and ")));
		}
		itinerary.legs().stream().filter(l -> l.trip().delayMinutes() > 0).findFirst().ifPresent(
				l -> notes.add(l.trip().vehicleName() + " is running " + l.trip().delayMinutes() + " min late"));
		int seats = itinerary.availableSeats();
		if (!itinerary.isBookableFor(passengers)) {
			notes.add("Not enough seats for " + passengers + (passengers == 1 ? " traveller" : " travellers"));
		}
		else if (seats <= LOW_SEAT_THRESHOLD) {
			notes.add("Only " + seats + " seats left");
		}
		return notes;
	}

	private static String minutes(long total) {
		return total >= 60 ? (total / 60) + "h" + (total % 60 == 0 ? "" : " " + (total % 60) + "m") : total + "m";
	}

	private static String capitalise(String text) {
		return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}
}
