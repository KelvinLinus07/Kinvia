package com.smarttransit.smart_transit.booking;

import java.math.BigDecimal;
import java.util.List;

import com.smarttransit.smart_transit.network.CoachClass;
import com.smarttransit.smart_transit.network.SeatType;

/** Seat availability of one trip between two stops, grouped by coach. */
public record SeatMapDto(long tripId, int boardSequence, int alightSequence, List<CoachMapDto> coaches) {

	public record CoachMapDto(long id, String code, CoachClass coachClass, String classLabel, int rows, int columns,
			int aisleAfter, BigDecimal fare, int availableSeats, List<SeatDto> seats) {
	}

	/** {@code mine} is true for seats the caller currently holds; {@code occupant} is only filled for operators. */
	public record SeatDto(long id, String label, int row, int column, SeatType type, SeatState state, boolean mine,
			String occupant) {
	}
}
