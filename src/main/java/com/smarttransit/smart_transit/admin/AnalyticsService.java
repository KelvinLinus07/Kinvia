package com.smarttransit.smart_transit.admin;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.admin.AnalyticsDtos.DailyTrend;
import com.smarttransit.smart_transit.admin.AnalyticsDtos.DashboardSummary;
import com.smarttransit.smart_transit.admin.AnalyticsDtos.OperatorDashboardSummary;
import com.smarttransit.smart_transit.admin.AnalyticsDtos.PopularRoute;
import com.smarttransit.smart_transit.booking.BookingRepository;
import com.smarttransit.smart_transit.booking.BookingStatus;
import com.smarttransit.smart_transit.network.OperatorRepository;
import com.smarttransit.smart_transit.network.RouteRepository;
import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.network.VehicleRepository;
import com.smarttransit.smart_transit.payment.PaymentRepository;
import com.smarttransit.smart_transit.schedule.TripRepository;
import com.smarttransit.smart_transit.schedule.TripStatus;
import com.smarttransit.smart_transit.user.Role;
import com.smarttransit.smart_transit.user.UserRepository;

/** Read-only counts and simple aggregates for the admin and operator dashboards; nothing here is invented. */
@Service
public class AnalyticsService {

	private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

	private final UserRepository users;
	private final OperatorRepository operators;
	private final BookingRepository bookings;
	private final PaymentRepository payments;
	private final TripRepository trips;
	private final RouteRepository routes;
	private final VehicleRepository vehicles;
	private final Clock clock;

	public AnalyticsService(UserRepository users, OperatorRepository operators, BookingRepository bookings,
			PaymentRepository payments, TripRepository trips, RouteRepository routes, VehicleRepository vehicles,
			Clock clock) {
		this.users = users;
		this.operators = operators;
		this.bookings = bookings;
		this.payments = payments;
		this.trips = trips;
		this.routes = routes;
		this.vehicles = vehicles;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public DashboardSummary systemDashboard() {
		LocalDate today = LocalDate.now(clock);
		Instant since = today.minusDays(13).atStartOfDay(ZoneOffset.UTC).toInstant();

		Map<String, Long> transportDistribution = new LinkedHashMap<>();
		for (TransportMode mode : TransportMode.values()) {
			transportDistribution.put(mode.name(), trips.countByServiceDateAndStatusIn(today,
					List.of(TripStatus.SCHEDULED, TripStatus.DELAYED, TripStatus.DEPARTED)));
		}

		Map<String, long[]> byDay = new TreeMap<>();
		Map<String, BigDecimal> revenueByDay = new TreeMap<>();
		for (Object[] row : bookings.findTrendSince(since)) {
			String day = DAY.format(((Instant) row[0]).atZone(ZoneOffset.UTC).toLocalDate());
			byDay.merge(day, new long[] { 1 }, (a, b) -> new long[] { a[0] + 1 });
			revenueByDay.merge(day, (BigDecimal) row[1], BigDecimal::add);
		}
		List<DailyTrend> trend = byDay.entrySet().stream()
				.map(e -> new DailyTrend(e.getKey(), e.getValue()[0], revenueByDay.getOrDefault(e.getKey(), BigDecimal.ZERO)))
				.toList();

		return new DashboardSummary(users.count(), users.countByRole(Role.PASSENGER), operators.count(),
				bookings.count(), bookings.countByStatus(BookingStatus.CONFIRMED),
				trips.countByServiceDateAndStatusIn(today, List.of(TripStatus.SCHEDULED, TripStatus.DELAYED, TripStatus.DEPARTED)),
				payments.totalCollected(), payments.totalRefunded(), transportDistribution, List.of(), trend);
	}

	@Transactional(readOnly = true)
	public OperatorDashboardSummary operatorDashboard(Long operatorId) {
		LocalDate today = LocalDate.now(clock);
		long vehicleCount = vehicles.findIds(operatorId, null, org.springframework.data.domain.PageRequest.of(0, 1))
				.getTotalElements();
		long routeCount = routes.findScoped(operatorId, org.springframework.data.domain.PageRequest.of(0, 1))
				.getTotalElements();
		long tripsToday = trips.findScopedOnDate(operatorId, today, org.springframework.data.domain.PageRequest.of(0, 1))
				.getTotalElements();
		long upcomingBookings = bookings.findForOperator(operatorId, BookingStatus.CONFIRMED,
				org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
		return new OperatorDashboardSummary(vehicleCount, routeCount, tripsToday, upcomingBookings, BigDecimal.ZERO);
	}
}
