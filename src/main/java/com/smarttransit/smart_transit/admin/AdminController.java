package com.smarttransit.smart_transit.admin;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smarttransit.smart_transit.admin.AnalyticsDtos.DashboardSummary;
import com.smarttransit.smart_transit.booking.BookingDtos.BookingSummaryDto;
import com.smarttransit.smart_transit.booking.BookingService;
import com.smarttransit.smart_transit.booking.BookingStatus;
import com.smarttransit.smart_transit.common.PageResponse;
import com.smarttransit.smart_transit.network.NetworkAdminService;
import com.smarttransit.smart_transit.network.NetworkDtos.OperatorDto;
import com.smarttransit.smart_transit.network.NetworkDtos.OperatorRequest;
import com.smarttransit.smart_transit.network.NetworkDtos.StationDto;
import com.smarttransit.smart_transit.network.NetworkDtos.StationRequest;
import com.smarttransit.smart_transit.network.StationRepository;
import com.smarttransit.smart_transit.notification.NotificationDto;
import com.smarttransit.smart_transit.notification.NotificationService;
import com.smarttransit.smart_transit.notification.NotificationType;
import com.smarttransit.smart_transit.security.RequiresRole;
import com.smarttransit.smart_transit.user.Role;
import com.smarttransit.smart_transit.user.UserDtos.AssignRoleRequest;
import com.smarttransit.smart_transit.user.UserDtos.SetEnabledRequest;
import com.smarttransit.smart_transit.user.UserDtos.UserDto;
import com.smarttransit.smart_transit.user.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** System-wide management, restricted to admins. */
@RestController
@RequestMapping("/api/admin")
@RequiresRole(Role.ADMIN)
public class AdminController {

	private final AnalyticsService analytics;
	private final NetworkAdminService networkAdmin;
	private final StationRepository stations;
	private final UserService userService;
	private final BookingService bookingService;
	private final NotificationService notifications;

	public AdminController(AnalyticsService analytics, NetworkAdminService networkAdmin, StationRepository stations,
			UserService userService, BookingService bookingService, NotificationService notifications) {
		this.analytics = analytics;
		this.networkAdmin = networkAdmin;
		this.stations = stations;
		this.userService = userService;
		this.bookingService = bookingService;
		this.notifications = notifications;
	}

	@GetMapping("/dashboard")
	DashboardSummary dashboard() {
		return analytics.systemDashboard();
	}

	@GetMapping("/users")
	PageResponse<UserDto> users(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return userService.search(q, page, size);
	}

	@PutMapping("/users/{id}/role")
	UserDto assignRole(@PathVariable Long id, @Valid @RequestBody AssignRoleRequest request) {
		return userService.assignRole(id, request);
	}

	@PutMapping("/users/{id}/status")
	UserDto setEnabled(@PathVariable Long id, @Valid @RequestBody SetEnabledRequest request) {
		return userService.setEnabled(id, request.enabled());
	}

	@GetMapping("/stations")
	List<StationDto> stations() {
		return stations.findAll().stream().map(StationDto::from).toList();
	}

	@PostMapping("/stations")
	StationDto createStation(@Valid @RequestBody StationRequest request) {
		return networkAdmin.createStation(request);
	}

	@PutMapping("/stations/{id}")
	StationDto updateStation(@PathVariable Long id, @Valid @RequestBody StationRequest request) {
		return networkAdmin.updateStation(id, request);
	}

	@GetMapping("/operators")
	List<OperatorDto> operators() {
		return networkAdmin.listOperators();
	}

	@PostMapping("/operators")
	OperatorDto createOperator(@Valid @RequestBody OperatorRequest request) {
		return networkAdmin.createOperator(request);
	}

	@PutMapping("/operators/{id}")
	OperatorDto updateOperator(@PathVariable Long id, @Valid @RequestBody OperatorRequest request) {
		return networkAdmin.updateOperator(id, request);
	}

	@GetMapping("/bookings")
	PageResponse<BookingSummaryDto> bookings(@RequestParam(required = false) BookingStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return bookingService.adminList(status, page, size);
	}

	@GetMapping("/notifications")
	PageResponse<NotificationDto> notifications(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return notifications.recent(page, size);
	}

	record BroadcastRequest(@NotBlank @Size(max = 160) String title, @NotBlank @Size(max = 600) String message) {
	}

	@PostMapping("/notifications/broadcast")
	Map<String, Integer> broadcast(@Valid @RequestBody BroadcastRequest request) {
		return Map.of("recipients",
				notifications.broadcastToPassengers(NotificationType.SERVICE_ALERT, request.title(), request.message()));
	}
}
