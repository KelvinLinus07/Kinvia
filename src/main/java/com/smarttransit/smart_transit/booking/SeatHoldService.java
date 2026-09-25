package com.smarttransit.smart_transit.booking;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.config.KinviaProperties;
import com.smarttransit.smart_transit.network.RouteFare;
import com.smarttransit.smart_transit.network.RouteRepository;
import com.smarttransit.smart_transit.network.RouteStop;
import com.smarttransit.smart_transit.network.Seat;
import com.smarttransit.smart_transit.network.SeatRepository;
import com.smarttransit.smart_transit.schedule.Trip;
import com.smarttransit.smart_transit.schedule.TripRepository;
import com.smarttransit.smart_transit.security.CurrentUser;
import com.smarttransit.smart_transit.user.UserRepository;

/**
 * Temporarily claims seats while a traveller decides. Every hold takes a row lock on the trip first, so two people
 * can never be granted the same seat over overlapping stops: the second request waits, then sees the first.
 */
@Service
public class SeatHoldService {

	private final TripRepository trips;
	private final RouteRepository routes;
	private final SeatRepository seats;
	private final SeatReservationRepository reservations;
	private final UserRepository users;
	private final KinviaProperties properties;
	private final Clock clock;

	public SeatHoldService(TripRepository trips, RouteRepository routes, SeatRepository seats,
			SeatReservationRepository reservations, UserRepository users, KinviaProperties properties, Clock clock) {
		this.trips = trips;
		this.routes = routes;
		this.seats = seats;
		this.reservations = reservations;
		this.users = users;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public HoldDto hold(CurrentUser user, long tripId, long seatId, int board, int alight) {
		Trip trip = trips.lockById(tripId).orElseThrow(() -> ApiException.notFound("Trip", tripId));
		Instant now = Instant.now(clock);

		List<RouteStop> stops = routes.findStops(List.of(trip.getRoute().getId()));
		if (board < 0 || alight <= board || alight >= stops.size()) {
			throw ApiException.badRequest("Invalid boarding or alighting stop");
		}
		if (!trip.getStatus().isBookable()
				|| !trip.getDeparture().plusMinutes(stops.get(board).getDepartureOffsetMinutes())
						.isAfter(LocalDateTime.now(clock))) {
			throw ApiException.conflict("This service is no longer open for booking");
		}
		Seat seat = seats.findWithCoachById(seatId).orElseThrow(() -> ApiException.notFound("Seat", seatId));
		if (!seat.getCoach().getVehicle().getId().equals(trip.getVehicle().getId())) {
			throw ApiException.badRequest("This seat does not belong to the vehicle on this trip");
		}

		List<SeatReservation> conflicts = reservations.findConflicts(tripId, seatId, board, alight, now);
		if (!conflicts.isEmpty()) {
			return existingOwnHold(conflicts, user, seat);
		}
		if (reservations.countLooseHolds(user.id(), tripId, now) >= properties.booking().maxPassengers()) {
			throw ApiException.badRequest("You can hold at most " + properties.booking().maxPassengers()
					+ " seats on one service at a time");
		}

		RouteFare fare = routes.findFares(List.of(trip.getRoute().getId())).stream()
				.filter(f -> f.getCoachClass() == seat.getCoach().getCoachClass()).findFirst()
				.orElseThrow(() -> ApiException.conflict("This class is not sold on this route"));
		int distance = stops.get(alight).getDistanceKm() - stops.get(board).getDistanceKm();

		SeatReservation reservation = new SeatReservation(trip, seat, users.getReferenceById(user.id()), board, alight,
				fare.fareForDistance(distance), now.plus(properties.booking().holdDuration()));
		return HoldDto.from(reservations.save(reservation));
	}

	/** Re-selecting a seat you already hold is harmless; anyone else's claim is a conflict. */
	private HoldDto existingOwnHold(List<SeatReservation> conflicts, CurrentUser user, Seat seat) {
		SeatReservation own = conflicts.stream()
				.filter(r -> r.getUser().getId().equals(user.id()) && r.getBooking() == null
						&& r.getStatus() == ReservationStatus.HELD)
				.findFirst().orElseThrow(() -> ApiException.conflict("Seat " + seat.getLabel() + " was just taken"));
		return HoldDto.from(own);
	}

	@Transactional
	public void release(CurrentUser user, long holdId) {
		SeatReservation reservation = reservations.findByIdAndUserId(holdId, user.id())
				.orElseThrow(() -> ApiException.notFound("Hold", holdId));
		if (reservation.getBooking() != null) {
			throw ApiException.conflict("This seat belongs to a booking; cancel the booking instead");
		}
		reservation.release();
	}

	@Transactional(readOnly = true)
	public List<HoldDto> myHolds(CurrentUser user) {
		return reservations.findLooseHolds(user.id(), Instant.now(clock)).stream().map(HoldDto::from).toList();
	}
}
