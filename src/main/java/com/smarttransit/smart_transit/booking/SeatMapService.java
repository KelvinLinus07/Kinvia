package com.smarttransit.smart_transit.booking;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.booking.SeatMapDto.CoachMapDto;
import com.smarttransit.smart_transit.booking.SeatMapDto.SeatDto;
import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.network.Coach;
import com.smarttransit.smart_transit.network.CoachClass;
import com.smarttransit.smart_transit.network.CoachRepository;
import com.smarttransit.smart_transit.network.RouteFare;
import com.smarttransit.smart_transit.network.RouteRepository;
import com.smarttransit.smart_transit.network.RouteStop;
import com.smarttransit.smart_transit.schedule.Trip;
import com.smarttransit.smart_transit.schedule.TripRepository;
import com.smarttransit.smart_transit.security.CurrentUser;

@Service
public class SeatMapService {

	private final TripRepository trips;
	private final RouteRepository routes;
	private final CoachRepository coaches;
	private final SeatReservationRepository reservations;
	private final Clock clock;

	public SeatMapService(TripRepository trips, RouteRepository routes, CoachRepository coaches,
			SeatReservationRepository reservations, Clock clock) {
		this.trips = trips;
		this.routes = routes;
		this.coaches = coaches;
		this.reservations = reservations;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public SeatMapDto seatMap(long tripId, int board, int alight, CurrentUser viewer, boolean showOccupants) {
		Trip trip = trips.findDetailedById(tripId).orElseThrow(() -> ApiException.notFound("Trip", tripId));
		List<RouteStop> stops = routes.findStops(List.of(trip.getRoute().getId()));
		if (board < 0 || alight <= board || alight >= stops.size()) {
			throw ApiException.badRequest("Invalid boarding or alighting stop");
		}
		int distance = stops.get(alight).getDistanceKm() - stops.get(board).getDistanceKm();
		Map<CoachClass, RouteFare> fares = routes.findFares(List.of(trip.getRoute().getId())).stream()
				.collect(Collectors.toMap(RouteFare::getCoachClass, f -> f));

		Map<Long, SeatReservation> occupiedBySeat = new HashMap<>();
		for (SeatReservation r : reservations.findActiveOverlapping(tripId, board, alight, Instant.now(clock))) {
			occupiedBySeat.merge(r.getSeat().getId(), r, (a, b) -> a.getStatus() == ReservationStatus.BOOKED ? a : b);
		}

		List<CoachMapDto> result = coaches.findWithSeatsByVehicleId(trip.getVehicle().getId()).stream()
				.filter(c -> fares.containsKey(c.getCoachClass()))
				.map(c -> toCoachMap(c, fares.get(c.getCoachClass()), distance, occupiedBySeat, viewer, showOccupants))
				.toList();
		return new SeatMapDto(tripId, board, alight, result);
	}

	private CoachMapDto toCoachMap(Coach coach, RouteFare fare, int distance, Map<Long, SeatReservation> occupied,
			CurrentUser viewer, boolean showOccupants) {
		List<SeatDto> seats = coach.getSeats().stream().map(seat -> {
			SeatReservation r = occupied.get(seat.getId());
			SeatState state = r == null ? SeatState.AVAILABLE
					: r.getStatus() == ReservationStatus.BOOKED ? SeatState.BOOKED : SeatState.HELD;
			boolean mine = r != null && viewer != null && r.getUser().getId().equals(viewer.id())
					&& state == SeatState.HELD;
			String occupant = showOccupants && r != null && r.getPassenger() != null ? r.getPassenger().getFullName()
					: null;
			return new SeatDto(seat.getId(), seat.getLabel(), seat.getRowIndex(), seat.getColumnIndex(),
					seat.getSeatType(), state, mine, occupant);
		}).toList();
		int available = (int) seats.stream().filter(s -> s.state() == SeatState.AVAILABLE).count();
		return new CoachMapDto(coach.getId(), coach.getCode(), coach.getCoachClass(), coach.getCoachClass().label(),
				coach.getSeatRows(), coach.getSeatColumns(), coach.getAisleAfter(), fare.fareForDistance(distance),
				available, seats);
	}
}
