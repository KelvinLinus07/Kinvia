package com.smarttransit.smart_transit.booking;

/** A seat that is currently held or booked over a section of a trip. */
public record ActiveSeat(long tripId, long seatId, long coachId, int boardSequence, int alightSequence) {

	public boolean overlaps(int board, int alight) {
		return boardSequence < alight && board < alightSequence;
	}
}
