package com.smarttransit.smart_transit.journey;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.smarttransit.smart_transit.network.TransportMode;

/** A complete way of getting from the origin to the destination: one or more legs joined by transfers. */
public record Itinerary(List<Leg> legs) {

	public Leg first() {
		return legs.getFirst();
	}

	public Leg last() {
		return legs.getLast();
	}

	public LocalDateTime departure() {
		return first().departure();
	}

	public LocalDateTime arrival() {
		return last().arrival();
	}

	public Duration duration() {
		return Duration.between(first().departure(), last().arrival());
	}

	public int transfers() {
		return legs.size() - 1;
	}

	/** Time spent waiting at transfer stations. */
	public Duration waiting() {
		Duration total = Duration.ZERO;
		for (int i = 1; i < legs.size(); i++) {
			total = total.plus(waitBefore(i));
		}
		return total;
	}

	/** Scheduled wait between leg {@code index - 1} and leg {@code index}. */
	public Duration waitBefore(int index) {
		return Duration.between(legs.get(index - 1).arrival(), legs.get(index).departure());
	}

	public Set<TransportMode> modes() {
		return legs.stream().map(l -> l.trip().mode()).collect(Collectors.toSet());
	}

	public boolean isMultimodal() {
		return modes().size() > 1;
	}

	public boolean isBookableFor(int passengers) {
		return legs.stream().allMatch(l -> l.isBookableFor(passengers));
	}

	public int availableSeats() {
		return legs.stream().mapToInt(Leg::totalAvailableSeats).min().orElse(0);
	}

	/** Per-passenger fare using the cheapest class that fits the party on each leg (lowest fare if sold out). */
	public BigDecimal farePerPassenger(int passengers) {
		return legs.stream().map(l -> l.cheapestFor(passengers).map(ClassOffer::fare).orElseGet(l::lowestFare))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/** Duration-weighted comfort across legs (0-100). */
	public double comfortFor(int passengers) {
		long totalMinutes = legs.stream().mapToLong(l -> l.duration().toMinutes()).sum();
		if (totalMinutes == 0) {
			return 0;
		}
		double weighted = legs.stream().mapToDouble(l -> l.comfortFor(passengers) * l.duration().toMinutes()).sum();
		return weighted / totalMinutes;
	}

	public String key() {
		return JourneyKey.format(legs.stream().map(l -> new SegmentRef(l.trip().tripId(), l.boardIndex(), l.alightIndex())).toList());
	}
}
