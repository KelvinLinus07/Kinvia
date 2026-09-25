package com.smarttransit.smart_transit.journey;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.smarttransit.smart_transit.network.CoachClass;
import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.schedule.TripStatus;

class JourneyFilterTest {

	// Cheapest class costs 250 per traveller (a 40-seat class at 250, and a dearer 4-seat one at 900).
	private final Itinerary morningTrain = itinerary("09:30", new ClassOffer(CoachClass.SLEEPER, new BigDecimal("250"), 40),
			new ClassOffer(CoachClass.AC_2_TIER, new BigDecimal("900"), 4));

	@Test
	void noFilterAcceptsEverything() {
		assertFalse(JourneyFilter.NONE.isActive());
		assertTrue(JourneyFilter.NONE.accepts(morningTrain, 1));
	}

	@Test
	void departureTimeMustFallInsideTheWindow() {
		JourneyFilter morning = new JourneyFilter(TimeOfDay.MORNING.window(), null);
		JourneyFilter evening = new JourneyFilter(TimeOfDay.EVENING.window(), null);

		assertTrue(morning.accepts(morningTrain, 1));
		assertFalse(evening.accepts(morningTrain, 1));
	}

	@Test
	void budgetIsComparedWithTheFarePerTraveller() {
		assertTrue(new JourneyFilter(null, new BigDecimal("250")).accepts(morningTrain, 1), "equal to the budget is fine");
		assertFalse(new JourneyFilter(null, new BigDecimal("249")).accepts(morningTrain, 1));
	}

	@Test
	void budgetFollowsTheClassThePartyCanActuallyBook() {
		// Five travellers no longer fit in the 4-seat class, but the cheap 40-seat class still fits them.
		assertTrue(new JourneyFilter(null, new BigDecimal("300")).accepts(morningTrain, 5));

		Itinerary onlyDearClassFree = itinerary("09:30", new ClassOffer(CoachClass.SLEEPER, new BigDecimal("250"), 2),
				new ClassOffer(CoachClass.AC_2_TIER, new BigDecimal("900"), 40));
		assertFalse(new JourneyFilter(null, new BigDecimal("300")).accepts(onlyDearClassFree, 5),
				"with 5 travellers only the 900 class has room");
	}

	@Test
	void bothLimitsMustHold() {
		JourneyFilter filter = new JourneyFilter(TimeOfDay.MORNING.window(), new BigDecimal("300"));

		assertTrue(filter.accepts(morningTrain, 1));
		assertFalse(new JourneyFilter(TimeOfDay.MORNING.window(), new BigDecimal("100")).accepts(morningTrain, 1));
		assertFalse(new JourneyFilter(new DepartureWindow(LocalTime.of(13, 0), LocalTime.of(14, 0)),
				new BigDecimal("300")).accepts(morningTrain, 1));
	}

	private static Itinerary itinerary(String departure, ClassOffer... offers) {
		LocalDateTime dep = LocalDateTime.parse("2026-10-10T" + departure + ":00");
		StopTime origin = new StopTime(1, "AAA", "Alpha", "Alpha", 0, 0, 30, 0, dep, dep, 0);
		StopTime destination = new StopTime(2, "BBB", "Beta", "Beta", 0, 0, 30, 1, dep.plusHours(3), dep.plusHours(3), 150);
		TripSnapshot trip = new TripSnapshot(1, TransportMode.TRAIN, "T00001", "Alpha to Beta", 1, "Operator", "T00001",
				"Test Express 0001", List.of(), TripStatus.SCHEDULED, 0, null, List.of(origin, destination));
		return new Itinerary(List.of(new Leg(trip, 0, 1, List.of(offers))));
	}
}
