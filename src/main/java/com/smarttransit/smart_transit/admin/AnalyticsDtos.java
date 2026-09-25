package com.smarttransit.smart_transit.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AnalyticsDtos {

	record DashboardSummary(long totalUsers, long totalPassengers, long totalOperators, long totalBookings,
			long confirmedBookings, long activeJourneysToday, BigDecimal totalRevenue, BigDecimal totalRefunded,
			Map<String, Long> transportDistribution, List<PopularRoute> popularRoutes, List<DailyTrend> bookingTrend) {
	}

	record PopularRoute(String routeName, String operatorName, long bookings) {
	}

	record DailyTrend(String date, long bookings, BigDecimal revenue) {
	}

	record OperatorDashboardSummary(long totalVehicles, long totalRoutes, long tripsToday, long upcomingBookings,
			BigDecimal revenue) {
	}
}
