package com.smarttransit.smart_transit.journey;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.booking.SeatOccupancy;
import com.smarttransit.smart_transit.booking.SeatReservationRepository;
import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.network.Coach;
import com.smarttransit.smart_transit.network.CoachClass;
import com.smarttransit.smart_transit.network.RouteFare;
import com.smarttransit.smart_transit.network.RouteRepository;
import com.smarttransit.smart_transit.network.RouteStop;
import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.network.Vehicle;
import com.smarttransit.smart_transit.network.VehicleRepository;
import com.smarttransit.smart_transit.schedule.Trip;
import com.smarttransit.smart_transit.schedule.TripRepository;

/**
 * Turns persisted trips into planner-ready {@link Leg}s, pricing each from the route's fare rules and counting
 * seats left per class. Data is loaded in a fixed number of queries regardless of how many trips are involved.
 */
@Service
@Transactional(readOnly = true)
public class LegCatalog {

	private final TripRepository trips;
	private final RouteRepository routes;
	private final VehicleRepository vehicles;
	private final SeatReservationRepository reservations;
	private final Clock clock;

	public LegCatalog(TripRepository trips, RouteRepository routes, VehicleRepository vehicles,
			SeatReservationRepository reservations, Clock clock) {
		this.trips = trips;
		this.routes = routes;
		this.vehicles = vehicles;
		this.reservations = reservations;
		this.clock = clock;
	}

	/** Every bookable ride between any two stops of trips that could serve a search for {@code date}. */
	public List<Leg> searchLegs(LocalDate date, Set<TransportMode> modes, long originId, long destinationId) {
		List<Trip> candidates = trips.findBookableBetween(date.minusDays(1), date.plusDays(2), modes, originId,
				destinationId);
		Loaded loaded = load(candidates);
		LocalDateTime now = LocalDateTime.now(clock);
		List<Leg> legs = new ArrayList<>();
		for (Trip trip : candidates) {
			TripContext context = loaded.contexts().get(trip.getId());
			List<StopTime> stops = context.snapshot().stops();
			for (int board = 0; board < stops.size() - 1; board++) {
				if (!stops.get(board).departure().isAfter(now)) {
					continue;
				}
				for (int alight = board + 1; alight < stops.size(); alight++) {
					legs.add(loaded.leg(context, board, alight));
				}
			}
		}
		return legs;
	}

	/** Rebuilds the exact legs named by a journey key, rejecting anything no longer bookable or connectable. */
	public List<Leg> resolve(List<SegmentRef> refs) {
		List<Long> tripIds = refs.stream().map(SegmentRef::tripId).distinct().toList();
		Map<Long, Trip> found = trips.findDetailedByIds(tripIds).stream()
				.collect(Collectors.toMap(Trip::getId, t -> t));
		Loaded loaded = load(found.values());
		LocalDateTime now = LocalDateTime.now(clock);
		List<Leg> legs = new ArrayList<>();
		for (SegmentRef ref : refs) {
			Trip trip = found.get(ref.tripId());
			if (trip == null) {
				throw ApiException.notFound("Trip", ref.tripId());
			}
			TripContext context = loaded.contexts().get(trip.getId());
			int stopCount = context.snapshot().stops().size();
			if (ref.alightSequence() >= stopCount) {
				throw ApiException.badRequest("Journey key is invalid");
			}
			Leg leg = loaded.leg(context, ref.boardSequence(), ref.alightSequence());
			if (!trip.getStatus().isBookable() || !leg.departure().isAfter(now)) {
				throw ApiException.conflict("Service " + trip.getVehicle().getName() + " is no longer available to book");
			}
			legs.add(leg);
		}
		for (int i = 1; i < legs.size(); i++) {
			if (!ItineraryPlanner.canConnect(legs.get(i - 1), legs.get(i))) {
				throw ApiException.conflict("These services no longer connect");
			}
		}
		return legs;
	}

	private Loaded load(Collection<Trip> tripList) {
		if (tripList.isEmpty()) {
			return new Loaded(Map.of(), new SeatOccupancy(List.of()));
		}
		List<Long> routeIds = tripList.stream().map(t -> t.getRoute().getId()).distinct().toList();
		List<Long> vehicleIds = tripList.stream().map(t -> t.getVehicle().getId()).distinct().toList();
		List<Long> tripIds = tripList.stream().map(Trip::getId).toList();

		Map<Long, List<RouteStop>> stopsByRoute = routes.findStops(routeIds).stream()
				.collect(Collectors.groupingBy(s -> s.getRoute().getId()));
		Map<Long, List<RouteFare>> faresByRoute = routes.findFares(routeIds).stream()
				.collect(Collectors.groupingBy(f -> f.getRoute().getId()));
		Map<Long, Vehicle> vehicleById = vehicles.findAllWithCoaches(vehicleIds).stream()
				.collect(Collectors.toMap(Vehicle::getId, v -> v));
		SeatOccupancy occupancy = new SeatOccupancy(reservations.findActiveForTrips(tripIds, Instant.now(clock)));

		Map<Long, TripContext> contexts = new HashMap<>();
		for (Trip trip : tripList) {
			Vehicle vehicle = vehicleById.get(trip.getVehicle().getId());
			Map<CoachClass, RouteFare> fares = new EnumMap<>(CoachClass.class);
			faresByRoute.getOrDefault(trip.getRoute().getId(), List.of()).forEach(f -> fares.put(f.getCoachClass(), f));
			Map<CoachClass, List<Coach>> coaches = vehicle.getCoaches().stream()
					.collect(Collectors.groupingBy(Coach::getCoachClass, () -> new EnumMap<>(CoachClass.class),
							Collectors.toList()));
			contexts.put(trip.getId(), new TripContext(snapshot(trip, vehicle,
					stopsByRoute.getOrDefault(trip.getRoute().getId(), List.of())), fares, coaches));
		}
		return new Loaded(contexts, occupancy);
	}

	private static TripSnapshot snapshot(Trip trip, Vehicle vehicle, List<RouteStop> routeStops) {
		List<StopTime> stops = routeStops.stream().map(rs -> new StopTime(rs.getStation().getId(),
				rs.getStation().getCode(), rs.getStation().getName(), rs.getStation().getCity(),
				rs.getStation().getLatitude(), rs.getStation().getLongitude(), rs.getStation().getTransferMinutes(),
				rs.getSequence(), trip.getDeparture().plusMinutes(rs.getArrivalOffsetMinutes()),
				trip.getDeparture().plusMinutes(rs.getDepartureOffsetMinutes()), rs.getDistanceKm())).toList();
		List<String> amenities = vehicle.getAmenities().isBlank() ? List.of()
				: Arrays.stream(vehicle.getAmenities().split(",")).map(String::trim).filter(a -> !a.isEmpty()).toList();
		return new TripSnapshot(trip.getId(), trip.getRoute().getMode(), trip.getRoute().getCode(),
				trip.getRoute().getName(), trip.getRoute().getOperator().getId(),
				trip.getRoute().getOperator().getName(), vehicle.getNumber(), vehicle.getName(), amenities,
				trip.getStatus(), trip.getDelayMinutes(), trip.getStatusNote(), stops);
	}

	private record TripContext(TripSnapshot snapshot, Map<CoachClass, RouteFare> fares,
			Map<CoachClass, List<Coach>> coaches) {
	}

	private record Loaded(Map<Long, TripContext> contexts, SeatOccupancy occupancy) {

		Leg leg(TripContext context, int board, int alight) {
			TripSnapshot trip = context.snapshot();
			int distance = trip.stops().get(alight).distanceKm() - trip.stops().get(board).distanceKm();
			List<ClassOffer> offers = new ArrayList<>();
			context.fares().forEach((coachClass, fare) -> {
				List<Coach> coaches = context.coaches().getOrDefault(coachClass, List.of());
				if (coaches.isEmpty()) {
					return;
				}
				int available = coaches.stream().mapToInt(c -> c.getCapacity()
						- occupancy.occupiedSeats(trip.tripId(), c.getId(), board, alight)).sum();
				BigDecimal price = fare.fareForDistance(distance);
				offers.add(new ClassOffer(coachClass, price, Math.max(0, available)));
			});
			return new Leg(trip, board, alight, List.copyOf(offers));
		}
	}
}
