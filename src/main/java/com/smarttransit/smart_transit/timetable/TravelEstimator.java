package com.smarttransit.smart_transit.timetable;

import com.smarttransit.smart_transit.network.TransportMode;

/**
 * Estimates distance and travel time between two stations. The imported workbook lists departure times but no
 * arrival times or distances, so these are estimates: great-circle distance stretched by a road/rail factor and
 * divided by a typical average speed. Replace with real figures if the timetable ever supplies them.
 */
public final class TravelEstimator {

	private static final double EARTH_RADIUS_KM = 6371.0;
	private static final double ROUTE_FACTOR = 1.25;
	private static final double TRAIN_KMH = 55;
	private static final double BUS_KMH = 45;
	private static final int MINIMUM_MINUTES = 15;
	private static final int ROUNDING_MINUTES = 5;

	private TravelEstimator() {
	}

	public record Estimate(int distanceKm, int minutes) {
	}

	public static Estimate estimate(TransportMode mode, double fromLat, double fromLng, double toLat, double toLng) {
		double km = greatCircleKm(fromLat, fromLng, toLat, toLng) * ROUTE_FACTOR;
		double speed = mode == TransportMode.TRAIN ? TRAIN_KMH : BUS_KMH;
		int rawMinutes = (int) Math.ceil(km / speed * 60);
		int minutes = Math.max(MINIMUM_MINUTES, rawMinutes);
		minutes = (int) (Math.ceil(minutes / (double) ROUNDING_MINUTES) * ROUNDING_MINUTES);
		return new Estimate(Math.max(1, (int) Math.round(km)), minutes);
	}

	static double greatCircleKm(double lat1, double lng1, double lat2, double lng2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
	}
}
