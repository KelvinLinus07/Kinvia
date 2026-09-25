package com.smarttransit.smart_transit.journey;

import java.util.List;

import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.schedule.TripStatus;

/** Immutable, persistence-free view of a trip used by planning and ranking. */
public record TripSnapshot(long tripId, TransportMode mode, String routeCode, String routeName, long operatorId,
		String operatorName, String vehicleNumber, String vehicleName, List<String> amenities, TripStatus status,
		int delayMinutes, String statusNote, List<StopTime> stops) {
}
