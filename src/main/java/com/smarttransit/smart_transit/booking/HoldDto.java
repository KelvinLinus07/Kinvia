package com.smarttransit.smart_transit.booking;

import java.math.BigDecimal;
import java.time.Instant;

import com.smarttransit.smart_transit.network.CoachClass;

public record HoldDto(Long id, Long tripId, Long seatId, String seatLabel, String coachCode, CoachClass coachClass,
		int boardSequence, int alightSequence, BigDecimal fare, Instant expiresAt) {

	public static HoldDto from(SeatReservation r) {
		return new HoldDto(r.getId(), r.getTrip().getId(), r.getSeat().getId(), r.getSeat().getLabel(),
				r.getSeat().getCoach().getCode(), r.getSeat().getCoach().getCoachClass(), r.getBoardSequence(),
				r.getAlightSequence(), r.getFare(), r.getExpiresAt());
	}
}
