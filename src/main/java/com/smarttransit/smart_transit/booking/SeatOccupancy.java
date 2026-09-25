package com.smarttransit.smart_transit.booking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** In-memory index answering "how many seats of this coach are taken between these stops of this trip?". */
public final class SeatOccupancy {

	private final Map<Long, List<ActiveSeat>> byTrip = new HashMap<>();

	public SeatOccupancy(List<ActiveSeat> active) {
		for (ActiveSeat seat : active) {
			byTrip.computeIfAbsent(seat.tripId(), k -> new java.util.ArrayList<>()).add(seat);
		}
	}

	public int occupiedSeats(long tripId, long coachId, int board, int alight) {
		Set<Long> seats = new HashSet<>();
		for (ActiveSeat seat : byTrip.getOrDefault(tripId, List.of())) {
			if (seat.coachId() == coachId && seat.overlaps(board, alight)) {
				seats.add(seat.seatId());
			}
		}
		return seats.size();
	}
}
