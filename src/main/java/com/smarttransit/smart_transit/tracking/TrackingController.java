package com.smarttransit.smart_transit.tracking;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.schedule.TripRepository;
import com.smarttransit.smart_transit.security.Public;

@RestController
@RequestMapping("/api/trips/{tripId}/tracking")
@Public
public class TrackingController {

	private final TripRepository trips;
	private final TrackingProvider provider;

	public TrackingController(TripRepository trips, TrackingProvider provider) {
		this.trips = trips;
		this.provider = provider;
	}

	@GetMapping
	TrackingStatus status(@PathVariable Long tripId) {
		return provider.statusOf(trips.findDetailedById(tripId).orElseThrow(() -> ApiException.notFound("Trip", tripId)));
	}
}
