package com.smarttransit.smart_transit.journey;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** A ride on one trip from one stop to a later stop, with the classes that can be booked for it. */
public record Leg(TripSnapshot trip, int boardIndex, int alightIndex, List<ClassOffer> offers) {

	public StopTime board() {
		return trip.stops().get(boardIndex);
	}

	public StopTime alight() {
		return trip.stops().get(alightIndex);
	}

	public LocalDateTime departure() {
		return board().departure();
	}

	public LocalDateTime arrival() {
		return alight().arrival();
	}

	/** Departure including any reported delay; used to judge whether a connection is still makeable. */
	public LocalDateTime expectedDeparture() {
		return departure().plusMinutes(trip.delayMinutes());
	}

	public LocalDateTime expectedArrival() {
		return arrival().plusMinutes(trip.delayMinutes());
	}

	public Duration duration() {
		return Duration.between(departure(), arrival());
	}

	public int distanceKm() {
		return alight().distanceKm() - board().distanceKm();
	}

	public List<StopTime> intermediateStops() {
		return trip.stops().subList(boardIndex + 1, alightIndex);
	}

	public boolean isBookableFor(int passengers) {
		return offers.stream().anyMatch(o -> o.availableSeats() >= passengers);
	}

	/** Cheapest class that still has room for the whole party. */
	public Optional<ClassOffer> cheapestFor(int passengers) {
		return offers.stream().filter(o -> o.availableSeats() >= passengers).min(Comparator.comparing(ClassOffer::fare));
	}

	public BigDecimal lowestFare() {
		return offers.stream().map(ClassOffer::fare).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
	}

	public int totalAvailableSeats() {
		return offers.stream().mapToInt(ClassOffer::availableSeats).sum();
	}

	/** Comfort of the best class the party could actually book, plus a small amenities bonus (0-100). */
	public int comfortFor(int passengers) {
		int classComfort = offers.stream().filter(o -> o.availableSeats() >= passengers)
				.mapToInt(o -> o.coachClass().comfort()).max()
				.orElseGet(() -> offers.stream().mapToInt(o -> o.coachClass().comfort()).max().orElse(0));
		return Math.min(100, classComfort + Math.min(10, trip.amenities().size() * 2));
	}
}
