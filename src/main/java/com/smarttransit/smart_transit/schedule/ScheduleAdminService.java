package com.smarttransit.smart_transit.schedule;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.common.PageResponse;
import com.smarttransit.smart_transit.network.NetworkDtos.ScheduleDto;
import com.smarttransit.smart_transit.network.NetworkDtos.ScheduleRequest;
import com.smarttransit.smart_transit.network.NetworkDtos.TripDto;
import com.smarttransit.smart_transit.network.NetworkDtos.TripStatusRequest;
import com.smarttransit.smart_transit.network.Route;
import com.smarttransit.smart_transit.network.RouteRepository;
import com.smarttransit.smart_transit.network.Vehicle;
import com.smarttransit.smart_transit.network.VehicleRepository;
import com.smarttransit.smart_transit.booking.Booking;
import com.smarttransit.smart_transit.booking.BookingRepository;
import com.smarttransit.smart_transit.booking.BookingService;
import com.smarttransit.smart_transit.notification.NotificationService;
import com.smarttransit.smart_transit.notification.NotificationType;
import com.smarttransit.smart_transit.security.CurrentUser;

@Service
public class ScheduleAdminService {

	private final ScheduleRepository schedules;
	private final TripRepository trips;
	private final RouteRepository routes;
	private final VehicleRepository vehicles;
	private final TripGenerator tripGenerator;
	private final BookingRepository bookings;
	private final BookingService bookingService;
	private final NotificationService notifications;

	public ScheduleAdminService(ScheduleRepository schedules, TripRepository trips, RouteRepository routes,
			VehicleRepository vehicles, TripGenerator tripGenerator, BookingRepository bookings,
			BookingService bookingService, NotificationService notifications) {
		this.schedules = schedules;
		this.trips = trips;
		this.routes = routes;
		this.vehicles = vehicles;
		this.tripGenerator = tripGenerator;
		this.bookings = bookings;
		this.bookingService = bookingService;
		this.notifications = notifications;
	}

	@Transactional(readOnly = true)
	public PageResponse<ScheduleDto> list(CurrentUser caller, int page, int size) {
		return PageResponse.of(schedules.findScoped(caller.operatorScope(), PageRequest.of(page, size)), ScheduleDto::from);
	}

	@Transactional
	public ScheduleDto create(CurrentUser caller, ScheduleRequest request) {
		Route route = routes.findById(request.routeId()).orElseThrow(() -> ApiException.notFound("Route", request.routeId()));
		caller.requireAccessTo(route.getOperator().getId());
		Vehicle vehicle = vehicles.findAllWithCoaches(List.of(request.vehicleId())).stream().findFirst()
				.orElseThrow(() -> ApiException.notFound("Vehicle", request.vehicleId()));
		if (!vehicle.getOperator().getId().equals(route.getOperator().getId())) {
			throw ApiException.badRequest("The vehicle must belong to the route's operator");
		}
		if (vehicle.getMode() != route.getMode()) {
			throw ApiException.badRequest("The vehicle mode must match the route mode");
		}
		Schedule schedule = schedules.save(new Schedule(route, vehicle, request.departureTime(), request.days()));
		tripGenerator.generate(LocalDate.now(), LocalDate.now().plusDays(30));
		return ScheduleDto.from(schedule);
	}

	@Transactional
	public ScheduleDto update(CurrentUser caller, Long id, ScheduleRequest request) {
		Schedule schedule = schedules.findById(id).orElseThrow(() -> ApiException.notFound("Schedule", id));
		caller.requireAccessTo(schedule.getRoute().getOperator().getId());
		Route route = routes.findById(request.routeId()).orElseThrow(() -> ApiException.notFound("Route", request.routeId()));
		caller.requireAccessTo(route.getOperator().getId());
		Vehicle vehicle = vehicles.findAllWithCoaches(List.of(request.vehicleId())).stream().findFirst()
				.orElseThrow(() -> ApiException.notFound("Vehicle", request.vehicleId()));
		schedule.update(route, vehicle, request.departureTime(), request.days(),
				request.active() == null || request.active());
		return ScheduleDto.from(schedule);
	}

	@Transactional(readOnly = true)
	public PageResponse<TripDto> listTrips(CurrentUser caller, LocalDate date, int page, int size) {
		return PageResponse.of(trips.findScopedOnDate(caller.operatorScope(), date, PageRequest.of(page, size)), TripDto::from);
	}

	@Transactional
	public TripDto updateTripStatus(CurrentUser caller, Long tripId, TripStatusRequest request) {
		Trip trip = trips.findDetailedById(tripId).orElseThrow(() -> ApiException.notFound("Trip", tripId));
		caller.requireAccessTo(trip.getRoute().getOperator().getId());
		TripStatus previous = trip.getStatus();
		trip.updateStatus(request.status(), request.delayMinutes(), request.note());
		notifyAffectedTravellers(trip, previous, request);
		return TripDto.from(trip);
	}

	/** Cancellations trigger a full refund; delays and alerts on already-booked trips are only announced. */
	private void notifyAffectedTravellers(Trip trip, TripStatus previous, TripStatusRequest request) {
		List<Booking> affected = bookings.findConfirmedOnTrip(trip.getId());
		if (affected.isEmpty()) {
			return;
		}
		if (request.status() == TripStatus.CANCELLED && previous != TripStatus.CANCELLED) {
			String reason = "Your service " + trip.getVehicle().getName() + " on " + trip.getServiceDate()
					+ " was cancelled by the operator.";
			affected.forEach(b -> bookingService.cancelForServiceChange(b.getId(), reason));
		}
		else if (request.status() == TripStatus.DELAYED && request.delayMinutes() > 0) {
			String message = trip.getVehicle().getName() + " on " + trip.getServiceDate() + " is now running "
					+ request.delayMinutes() + " minutes late" + (request.note() == null ? "." : ": " + request.note());
			affected.forEach(b -> notifications.send(b.getUser(), NotificationType.DELAY, "Service delayed", message, b));
		}
	}
}
