package com.smarttransit.smart_transit.tracking;

/**
 * Boundary to a live vehicle tracking feed. Replace {@link SimulatedTrackingProvider} with a real integration
 * (GPS feed, operator API) by implementing this interface and registering it as the primary bean; nothing else in
 * the tracking API changes.
 */
public interface TrackingProvider {

	TrackingStatus statusOf(com.smarttransit.smart_transit.schedule.Trip trip);

	boolean isLive();
}
