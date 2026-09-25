package com.smarttransit.smart_transit.journey;

import java.time.LocalDateTime;

/** A vehicle's scheduled visit to a station on one dated trip. */
public record StopTime(long stationId, String stationCode, String stationName, String city, double latitude,
		double longitude, int transferMinutes, int sequence, LocalDateTime arrival, LocalDateTime departure,
		int distanceKm) {
}
