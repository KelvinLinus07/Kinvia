package com.smarttransit.smart_transit.booking;

import java.math.BigDecimal;
import java.time.Instant;

import com.smarttransit.smart_transit.common.AuditedEntity;
import com.smarttransit.smart_transit.journey.JourneySegment;
import com.smarttransit.smart_transit.network.Seat;
import com.smarttransit.smart_transit.schedule.Trip;
import com.smarttransit.smart_transit.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A seat claimed on one trip between two stops. It starts as a short-lived HELD claim owned by a user, is attached
 * to a booking passenger when the booking is created, and becomes BOOKED when payment succeeds. A seat can be
 * reused by another traveller on a non-overlapping section of the same trip.
 */
@Entity
@Table(name = "seat_reservations", indexes = {
		@Index(name = "idx_reservations_trip_seat", columnList = "trip_id, seat_id"),
		@Index(name = "idx_reservations_status_expiry", columnList = "status, expires_at"),
		@Index(name = "idx_reservations_user", columnList = "user_id"),
		@Index(name = "idx_reservations_booking", columnList = "booking_id") })
public class SeatReservation extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	private Trip trip;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seat_id", nullable = false)
	private Seat seat;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private int boardSequence;

	@Column(nullable = false)
	private int alightSequence;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private ReservationStatus status = ReservationStatus.HELD;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal fare;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "booking_id")
	private Booking booking;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "booking_passenger_id")
	private BookingPassenger passenger;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "segment_id")
	private JourneySegment segment;

	protected SeatReservation() {
	}

	public SeatReservation(Trip trip, Seat seat, User user, int boardSequence, int alightSequence, BigDecimal fare,
			Instant expiresAt) {
		this.trip = trip;
		this.seat = seat;
		this.user = user;
		this.boardSequence = boardSequence;
		this.alightSequence = alightSequence;
		this.fare = fare;
		this.expiresAt = expiresAt;
	}

	public boolean isActiveAt(Instant now) {
		return status == ReservationStatus.BOOKED
				|| (status == ReservationStatus.HELD && expiresAt != null && expiresAt.isAfter(now));
	}

	public void attachTo(Booking booking, BookingPassenger passenger, JourneySegment segment, Instant expiresAt) {
		this.booking = booking;
		this.passenger = passenger;
		this.segment = segment;
		this.expiresAt = expiresAt;
	}

	public void markBooked() {
		status = ReservationStatus.BOOKED;
		expiresAt = null;
	}

	public void expire() {
		if (status == ReservationStatus.HELD) {
			status = ReservationStatus.EXPIRED;
		}
	}

	public void release() {
		if (status == ReservationStatus.HELD || status == ReservationStatus.BOOKED) {
			status = ReservationStatus.RELEASED;
		}
	}

	public Trip getTrip() { return trip; }

	public Seat getSeat() { return seat; }

	public User getUser() { return user; }

	public int getBoardSequence() { return boardSequence; }

	public int getAlightSequence() { return alightSequence; }

	public ReservationStatus getStatus() { return status; }

	public Instant getExpiresAt() { return expiresAt; }

	public BigDecimal getFare() { return fare; }

	public Booking getBooking() { return booking; }

	public BookingPassenger getPassenger() { return passenger; }

	public JourneySegment getSegment() { return segment; }
}
