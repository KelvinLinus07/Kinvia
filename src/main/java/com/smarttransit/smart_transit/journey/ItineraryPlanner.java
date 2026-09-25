package com.smarttransit.smart_transit.journey;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds every sensible way to connect two stations by chaining legs. It is a bounded depth-first search over legs
 * indexed by boarding station: a connection must respect the transfer station's minimum change time (using
 * delay-adjusted times) and a maximum wait, and a journey never revisits a station. This is intentionally simple
 * and exact for regional networks; a RAPTOR-style implementation can replace it behind the same signature.
 */
public final class ItineraryPlanner {

	private static final int SAFETY_LIMIT = 5_000;

	private final int maxTransfers;
	private final Duration maxWait;

	public ItineraryPlanner(int maxTransfers, Duration maxWait) {
		this.maxTransfers = maxTransfers;
		this.maxWait = maxWait;
	}

	public List<Itinerary> plan(List<Leg> legs, long originId, long destinationId, int passengers) {
		Map<Long, List<Leg>> byBoardingStation = new HashMap<>();
		for (Leg leg : legs) {
			byBoardingStation.computeIfAbsent(leg.board().stationId(), k -> new ArrayList<>()).add(leg);
		}
		List<Itinerary> results = new ArrayList<>();
		for (Leg first : byBoardingStation.getOrDefault(originId, List.of())) {
			if (!first.isBookableFor(passengers)) {
				continue;
			}
			Set<Long> visited = new HashSet<>(Set.of(originId));
			extend(new ArrayList<>(List.of(first)), visited, destinationId, passengers, byBoardingStation, results);
		}
		return results;
	}

	private void extend(List<Leg> path, Set<Long> visited, long destinationId, int passengers,
			Map<Long, List<Leg>> byBoardingStation, List<Itinerary> results) {
		Leg last = path.getLast();
		long arrivalStation = last.alight().stationId();
		if (arrivalStation == destinationId) {
			results.add(new Itinerary(List.copyOf(path)));
			return;
		}
		if (path.size() > maxTransfers || results.size() >= SAFETY_LIMIT || visited.contains(arrivalStation)) {
			return;
		}
		visited.add(arrivalStation);
		for (Leg next : byBoardingStation.getOrDefault(arrivalStation, List.of())) {
			if (isValidConnection(last, next, visited) && next.isBookableFor(passengers)) {
				path.add(next);
				extend(path, visited, destinationId, passengers, byBoardingStation, results);
				path.removeLast();
			}
		}
		visited.remove(arrivalStation);
	}

	private boolean isValidConnection(Leg arriving, Leg departing, Set<Long> visited) {
		if (visited.contains(departing.alight().stationId()) || !canConnect(arriving, departing)) {
			return false;
		}
		return Duration.between(arriving.expectedArrival(), departing.expectedDeparture()).compareTo(maxWait) <= 0;
	}

	/** Same station, different trips, and enough (delay-adjusted) time to change vehicles. */
	public static boolean canConnect(Leg arriving, Leg departing) {
		if (arriving.trip().tripId() == departing.trip().tripId()
				|| arriving.alight().stationId() != departing.board().stationId()) {
			return false;
		}
		Duration wait = Duration.between(arriving.expectedArrival(), departing.expectedDeparture());
		return wait.toMinutes() >= arriving.alight().transferMinutes();
	}
}
