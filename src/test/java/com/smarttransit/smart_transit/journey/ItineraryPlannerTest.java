package com.smarttransit.smart_transit.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.smarttransit.smart_transit.network.CoachClass;
import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.schedule.TripStatus;

/**
 * Exercises the multimodal planner against a small in-memory network built directly from records, with no Spring
 * context and no database: Siliguri --bus--> NJP --train--> Patna, plus a same-station direct alternative.
 */
class ItineraryPlannerTest {

	private static final long SILIGURI = 1, NJP = 2, PATNA = 3, GAYA = 4;

	@Test
	void chainsABusAndATrainAcrossACommonStation() {
		Leg bus = leg(100, TransportMode.BUS, SILIGURI, NJP, "18:00", "18:30", 30);
		Leg train = leg(200, TransportMode.TRAIN, NJP, PATNA, "20:15", "05:00", 30);

		List<Itinerary> results = new ItineraryPlanner(2, Duration.ofHours(6)).plan(List.of(bus, train), SILIGURI,
				PATNA, 1);

		assertEquals(1, results.size());
		Itinerary itinerary = results.getFirst();
		assertEquals(2, itinerary.legs().size());
		assertTrue(itinerary.isMultimodal());
		assertEquals(1, itinerary.transfers());
	}

	@Test
	void rejectsAConnectionThatDoesNotAllowEnoughTransferTime() {
		// NJP requires 30 minutes to change; this train leaves only 10 minutes after the bus arrives.
		Leg bus = leg(100, TransportMode.BUS, SILIGURI, NJP, "18:00", "18:30", 30);
		Leg tooSoonTrain = leg(200, TransportMode.TRAIN, NJP, PATNA, "18:40", "05:00", 30);

		List<Itinerary> results = new ItineraryPlanner(2, Duration.ofHours(6)).plan(List.of(bus, tooSoonTrain),
				SILIGURI, PATNA, 1);

		assertTrue(results.isEmpty());
	}

	@Test
	void respectsTheMaximumWaitBetweenLegs() {
		Leg bus = leg(100, TransportMode.BUS, SILIGURI, NJP, "06:00", "06:30", 30);
		Leg muchLaterTrain = leg(200, TransportMode.TRAIN, NJP, PATNA, "20:15", "05:00", 30);

		List<Itinerary> results = new ItineraryPlanner(2, Duration.ofHours(2)).plan(List.of(bus, muchLaterTrain),
				SILIGURI, PATNA, 1);

		assertTrue(results.isEmpty(), "a 13-hour wait exceeds the 2-hour cap and should not be offered");
	}

	@Test
	void skipsALegThatDoesNotHaveEnoughSeatsForTheParty() {
		Leg direct = new Leg(snapshot(300, TransportMode.BUS, SILIGURI, PATNA, "09:00", "17:00", 30),
				0, 1, List.of(new ClassOffer(CoachClass.AC_SEATER, new BigDecimal("500"), 1)));

		List<Itinerary> results = new ItineraryPlanner(2, Duration.ofHours(6)).plan(List.of(direct), SILIGURI, PATNA, 3);

		assertTrue(results.isEmpty(), "only 1 seat is free but 3 passengers are travelling");
	}

	@Test
	void aDirectLegNeedsNoTransferAndReportsZeroTransfers() {
		Leg direct = leg(300, TransportMode.BUS, SILIGURI, PATNA, "09:00", "17:00", 30);

		List<Itinerary> results = new ItineraryPlanner(2, Duration.ofHours(6)).plan(List.of(direct), SILIGURI, PATNA, 1);

		assertEquals(1, results.size());
		assertEquals(0, results.getFirst().transfers());
	}

	private static Leg leg(long tripId, TransportMode mode, long from, long to, String dep, String arr, int transferMinutes) {
		return new Leg(snapshot(tripId, mode, from, to, dep, arr, transferMinutes), 0, 1,
				List.of(new ClassOffer(CoachClass.AC_SEATER, new BigDecimal("500"), 40)));
	}

	private static TripSnapshot snapshot(long tripId, TransportMode mode, long from, long to, String dep, String arr,
			int transferMinutes) {
		LocalDateTime departure = LocalDateTime.parse("2026-10-10T" + dep + ":00");
		LocalDateTime arrival = arr.compareTo(dep) < 0 ? LocalDateTime.parse("2026-10-11T" + arr + ":00")
				: LocalDateTime.parse("2026-10-10T" + arr + ":00");
		StopTime origin = new StopTime(from, "S" + from, "Station " + from, "City", 0, 0, transferMinutes, 0,
				departure, departure, 0);
		StopTime destination = new StopTime(to, "S" + to, "Station " + to, "City", 0, 0, transferMinutes, 1, arrival,
				arrival, 100);
		return new TripSnapshot(tripId, mode, "R" + tripId, "Route " + tripId, 1, "Operator", "V" + tripId,
				"Vehicle " + tripId, List.of(), TripStatus.SCHEDULED, 0, null, List.of(origin, destination));
	}
}
