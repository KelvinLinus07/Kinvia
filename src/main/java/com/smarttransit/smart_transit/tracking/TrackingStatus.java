package com.smarttransit.smart_transit.tracking;

import java.time.LocalDateTime;

import com.smarttransit.smart_transit.schedule.TripStatus;

/** A point-in-time read of where a trip is and how it is running. */
public record TrackingStatus(long tripId, TripStatus status, int delayMinutes, Double progressPercent,
		String currentLocation, LocalDateTime estimatedArrival, String note, boolean live) {
}
