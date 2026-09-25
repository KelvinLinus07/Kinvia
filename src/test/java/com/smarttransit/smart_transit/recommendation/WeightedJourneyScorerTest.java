package com.smarttransit.smart_transit.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.smarttransit.smart_transit.journey.ClassOffer;
import com.smarttransit.smart_transit.journey.Itinerary;
import com.smarttransit.smart_transit.journey.Leg;
import com.smarttransit.smart_transit.journey.StopTime;
import com.smarttransit.smart_transit.journey.TripSnapshot;
import com.smarttransit.smart_transit.network.CoachClass;
import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.schedule.TripStatus;

class WeightedJourneyScorerTest {

	private final WeightedJourneyScorer scorer = new WeightedJourneyScorer();

	@Test
	void cheapestStrategyRanksTheLowestFareFirst() {
		Itinerary expensive = directItinerary(1, "09:00", "12:00", "1500");
		Itinerary cheap = directItinerary(2, "09:00", "15:00", "500");

		List<ScoredItinerary> ranked = scorer.rank(List.of(expensive, cheap), new RankingContext(RankingStrategy.CHEAPEST, 1));

		assertEquals(cheap, ranked.get(0).itinerary());
		assertTrue(ranked.get(0).tags().contains(JourneyTag.RECOMMENDED));
		assertTrue(ranked.get(0).tags().contains(JourneyTag.CHEAPEST));
	}

	@Test
	void fastestStrategyRanksTheShortestDurationFirst() {
		Itinerary slow = directItinerary(1, "09:00", "18:00", "500");
		Itinerary fast = directItinerary(2, "09:00", "11:00", "1500");

		List<ScoredItinerary> ranked = scorer.rank(List.of(slow, fast), new RankingContext(RankingStrategy.FASTEST, 1));

		assertEquals(fast, ranked.get(0).itinerary());
		assertTrue(ranked.get(0).tags().contains(JourneyTag.FASTEST));
	}

	@Test
	void unbookableItinerariesAlwaysSortAfterBookableOnes() {
		Itinerary tooFewSeats = directItinerary(1, "09:00", "11:00", "500", 1);
		Itinerary roomy = directItinerary(2, "09:00", "20:00", "3000", 40);

		List<ScoredItinerary> ranked = scorer.rank(List.of(tooFewSeats, roomy), new RankingContext(RankingStrategy.FASTEST, 4));

		assertEquals(roomy, ranked.get(0).itinerary(), "the faster option cannot seat 4 passengers, so it must rank after the bookable one");
	}

	@Test
	void emptyCandidateListProducesNoResults() {
		assertTrue(scorer.rank(List.of(), new RankingContext(RankingStrategy.BALANCED, 1)).isEmpty());
	}

	private static Itinerary directItinerary(long tripId, String departure, String arrival, String fare) {
		return directItinerary(tripId, departure, arrival, fare, 40);
	}

	private static Itinerary directItinerary(long tripId, String departure, String arrival, String fare, int seats) {
		LocalDateTime dep = LocalDateTime.parse("2026-10-10T" + departure + ":00");
		LocalDateTime arr = LocalDateTime.parse("2026-10-10T" + arrival + ":00");
		StopTime origin = new StopTime(1, "A", "Origin", "City", 0, 0, 20, 0, dep, dep, 0);
		StopTime destination = new StopTime(2, "B", "Destination", "City", 0, 0, 20, 1, arr, arr, 300);
		TripSnapshot trip = new TripSnapshot(tripId, TransportMode.BUS, "R" + tripId, "Route", 1, "Operator",
				"V" + tripId, "Vehicle", List.of(), TripStatus.SCHEDULED, 0, null, List.of(origin, destination));
		Leg leg = new Leg(trip, 0, 1, List.of(new ClassOffer(CoachClass.AC_SEATER, new BigDecimal(fare), seats)));
		return new Itinerary(List.of(leg));
	}
}
