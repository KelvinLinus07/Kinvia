package com.smarttransit.smart_transit.operator;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smarttransit.smart_transit.admin.AnalyticsDtos.OperatorDashboardSummary;
import com.smarttransit.smart_transit.admin.AnalyticsService;
import com.smarttransit.smart_transit.booking.BookingDtos.BookingSummaryDto;
import com.smarttransit.smart_transit.booking.BookingService;
import com.smarttransit.smart_transit.booking.BookingStatus;
import com.smarttransit.smart_transit.common.PageResponse;
import com.smarttransit.smart_transit.network.NetworkAdminService;
import com.smarttransit.smart_transit.network.NetworkDtos.RouteDto;
import com.smarttransit.smart_transit.network.NetworkDtos.RouteRequest;
import com.smarttransit.smart_transit.network.NetworkDtos.ScheduleDto;
import com.smarttransit.smart_transit.network.NetworkDtos.ScheduleRequest;
import com.smarttransit.smart_transit.network.NetworkDtos.TripDto;
import com.smarttransit.smart_transit.network.NetworkDtos.TripStatusRequest;
import com.smarttransit.smart_transit.network.NetworkDtos.VehicleDto;
import com.smarttransit.smart_transit.network.NetworkDtos.VehicleRequest;
import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.schedule.ScheduleAdminService;
import com.smarttransit.smart_transit.security.CurrentUser;
import com.smarttransit.smart_transit.security.RequiresRole;
import com.smarttransit.smart_transit.user.Role;

import jakarta.validation.Valid;

/**
 * Operator self-service: manage fleet, routes, schedules and trips, and view bookings and analytics scoped to the
 * caller's own operator. Admins may also call these endpoints by specifying {@code operatorId} where required.
 */
@RestController
@RequestMapping("/api/operator")
@RequiresRole({ Role.OPERATOR, Role.ADMIN })
public class OperatorController {

	private final NetworkAdminService networkAdmin;
	private final ScheduleAdminService scheduleAdmin;
	private final BookingService bookingService;
	private final AnalyticsService analytics;

	public OperatorController(NetworkAdminService networkAdmin, ScheduleAdminService scheduleAdmin,
			BookingService bookingService, AnalyticsService analytics) {
		this.networkAdmin = networkAdmin;
		this.scheduleAdmin = scheduleAdmin;
		this.bookingService = bookingService;
		this.analytics = analytics;
	}

	@GetMapping("/dashboard")
	OperatorDashboardSummary dashboard(CurrentUser user) {
		return analytics.operatorDashboard(user.operatorScope());
	}

	@GetMapping("/vehicles")
	PageResponse<VehicleDto> vehicles(CurrentUser user, @RequestParam(required = false) TransportMode mode,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return networkAdmin.listVehicles(user, mode, page, size);
	}

	@PostMapping("/vehicles")
	VehicleDto createVehicle(CurrentUser user, @Valid @RequestBody VehicleRequest request) {
		return networkAdmin.createVehicle(user, request);
	}

	@PutMapping("/vehicles/{id}")
	VehicleDto updateVehicle(CurrentUser user, @PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
		return networkAdmin.updateVehicle(user, id, request);
	}

	@GetMapping("/routes")
	PageResponse<RouteDto> routes(CurrentUser user, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return networkAdmin.listRoutes(user, page, size);
	}

	@PostMapping("/routes")
	RouteDto createRoute(CurrentUser user, @Valid @RequestBody RouteRequest request) {
		return networkAdmin.createRoute(user, request);
	}

	@PutMapping("/routes/{id}")
	RouteDto updateRoute(CurrentUser user, @PathVariable Long id, @Valid @RequestBody RouteRequest request) {
		return networkAdmin.updateRoute(user, id, request);
	}

	@GetMapping("/schedules")
	PageResponse<ScheduleDto> schedules(CurrentUser user, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return scheduleAdmin.list(user, page, size);
	}

	@PostMapping("/schedules")
	ScheduleDto createSchedule(CurrentUser user, @Valid @RequestBody ScheduleRequest request) {
		return scheduleAdmin.create(user, request);
	}

	@PutMapping("/schedules/{id}")
	ScheduleDto updateSchedule(CurrentUser user, @PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
		return scheduleAdmin.update(user, id, request);
	}

	@GetMapping("/trips")
	PageResponse<TripDto> trips(CurrentUser user, @RequestParam LocalDate date,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return scheduleAdmin.listTrips(user, date, page, size);
	}

	@PutMapping("/trips/{id}/status")
	TripDto updateTripStatus(CurrentUser user, @PathVariable Long id, @Valid @RequestBody TripStatusRequest request) {
		return scheduleAdmin.updateTripStatus(user, id, request);
	}

	@GetMapping("/bookings")
	PageResponse<BookingSummaryDto> bookings(CurrentUser user, @RequestParam(required = false) BookingStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return bookingService.operatorList(user.operatorScope(), status, page, size);
	}
}
