package com.smarttransit.smart_transit.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.smarttransit.smart_transit.network.CoachClass;
import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.schedule.TripStatus;

class ItineraryFareAndDurationTest {

	@Test
	void multiLegFareIsTheSumOfEachLegsCheapestBookableClass() {
		Leg leg1 = leg(1, "09:00", "12:00", TransportMode.BUS, offer(CoachClass.AC_SEATER, "300", 10));
		Leg leg2 = leg(2, "13:00", "20:00", TransportMode.TRAIN, offer(CoachClass.SLEEPER, "400", 10),
				offer(CoachClass.AC_3_TIER, "700", 10));
		Itinerary itinerary = new Itinerary(List.of(leg1, leg2));

		// Cheapest bookable class per leg: 300 (bus) + 400 (sleeper) = 700
		assertEquals(new BigDecimal("700"), itinerary.farePerPassenger(1));
	}

	@Test
	void durationSpansFirstDepartureToLastArrivalIncludingWaitTime() {
		Leg leg1 = leg(1, "09:00", "10:00", TransportMode.BUS, offer(CoachClass.AC_SEATER, "300", 10));
		Leg leg2 = leg(2, "11:30", "14:00", TransportMode.TRAIN, offer(CoachClass.SLEEPER, "400", 10));
		Itinerary itinerary = new Itinerary(List.of(leg1, leg2));

		assertEquals(300, itinerary.duration().toMinutes(), "09:00 to 14:00 is 5 hours");
		assertEquals(90, itinerary.waitBefore(1).toMinutes(), "10:00 to 11:30 is 90 minutes");
	}

	@Test
	void aSingleModeItineraryIsNotMultimodal() {
		Leg leg1 = leg(1, "09:00", "10:00", TransportMode.BUS, offer(CoachClass.AC_SEATER, "300", 10));
		assertFalse(new Itinerary(List.of(leg1)).isMultimodal());
	}

	@Test
	void aMixedModeItineraryIsMultimodal() {
		Leg leg1 = leg(1, "09:00", "10:00", TransportMode.BUS, offer(CoachClass.AC_SEATER, "300", 10));
		Leg leg2 = leg(2, "11:00", "14:00", TransportMode.TRAIN, offer(CoachClass.SLEEPER, "400", 10));
		assertTrue(new Itinerary(List.of(leg1, leg2)).isMultimodal());
	}

	private static ClassOffer offer(CoachClass coachClass, String fare, int seats) {
		return new ClassOffer(coachClass, new BigDecimal(fare), seats);
	}

	private static Leg leg(long tripId, String dep, String arr, TransportMode mode, ClassOffer... offers) {
		LocalDateTime departure = LocalDateTime.parse("2026-10-10T" + dep + ":00");
		LocalDateTime arrival = LocalDateTime.parse("2026-10-10T" + arr + ":00");
		StopTime origin = new StopTime(1, "A", "Origin", "City", 0, 0, 20, 0, departure, departure, 0);
		StopTime destination = new StopTime(2, "B", "Destination", "City", 0, 0, 20, 1, arrival, arrival, 300);
		TripSnapshot trip = new TripSnapshot(tripId, mode, "R" + tripId, "Route", 1, "Operator", "V" + tripId,
				"Vehicle", List.of(), TripStatus.SCHEDULED, 0, null, List.of(origin, destination));
		return new Leg(trip, 0, 1, List.of(offers));
	}
}
