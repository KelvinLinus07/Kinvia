package com.smarttransit.smart_transit.tracking;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.smarttransit.smart_transit.network.RouteRepository;
import com.smarttransit.smart_transit.network.RouteStop;
import com.smarttransit.smart_transit.schedule.Trip;
import com.smarttransit.smart_transit.schedule.TripStatus;

/**
 * Development stand-in for a real tracking feed: it derives an honest "estimated" position purely from the
 * schedule, the reported delay and the current time — no GPS, no external calls. Every response is explicitly
 * marked {@code live = false} so the frontend never presents it as real-time data.
 */
@Component
public class SimulatedTrackingProvider implements TrackingProvider {

	private final RouteRepository routes;
	private final Clock clock;

	public SimulatedTrackingProvider(RouteRepository routes, Clock clock) {
		this.routes = routes;
		this.clock = clock;
	}

	@Override
	public TrackingStatus statusOf(Trip trip) {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime departure = trip.getDeparture().plusMinutes(trip.getDelayMinutes());
		List<RouteStop> stops = routes.findStops(List.of(trip.getRoute().getId()));
		LocalDateTime arrival = departure.plusMinutes(stops.isEmpty() ? 0
				: stops.getLast().getArrivalOffsetMinutes() - stops.getFirst().getArrivalOffsetMinutes());

		if (trip.getStatus() == TripStatus.CANCELLED) {
			return new TrackingStatus(trip.getId(), trip.getStatus(), 0, null, null, null, trip.getStatusNote(), false);
		}
		if (now.isBefore(departure)) {
			return new TrackingStatus(trip.getId(), trip.getStatus(), trip.getDelayMinutes(), 0.0,
					stops.isEmpty() ? null : stops.getFirst().getStation().getName(), arrival, trip.getStatusNote(), false);
		}
		if (!now.isBefore(arrival)) {
			return new TrackingStatus(trip.getId(), TripStatus.COMPLETED, trip.getDelayMinutes(), 100.0,
					stops.isEmpty() ? null : stops.getLast().getStation().getName(), arrival, trip.getStatusNote(), false);
		}
		double totalMinutes = java.time.Duration.between(departure, arrival).toMinutes();
		double elapsed = java.time.Duration.between(departure, now).toMinutes();
		double progress = totalMinutes <= 0 ? 100.0 : Math.min(100.0, Math.max(0.0, 100.0 * elapsed / totalMinutes));
		String location = nearestStop(stops, progress);
		return new TrackingStatus(trip.getId(), trip.getStatus(), trip.getDelayMinutes(), round(progress), location,
				arrival, trip.getStatusNote(), false);
	}

	private static String nearestStop(List<RouteStop> stops, double progressPercent) {
		if (stops.isEmpty()) {
			return null;
		}
		int totalOffset = stops.getLast().getArrivalOffsetMinutes() - stops.getFirst().getArrivalOffsetMinutes();
		int targetOffset = stops.getFirst().getArrivalOffsetMinutes() + (int) (totalOffset * progressPercent / 100.0);
		RouteStop nearest = stops.getFirst();
		for (RouteStop stop : stops) {
			if (stop.getArrivalOffsetMinutes() <= targetOffset) {
				nearest = stop;
			}
		}
		return "Approaching/near " + nearest.getStation().getName();
	}

	private static double round(double value) {
		return Math.round(value * 10) / 10.0;
	}

	@Override
	public boolean isLive() {
		return false;
	}
}
