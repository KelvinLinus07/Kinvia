package com.smarttransit.smart_transit.config;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "kinvia")
public record KinviaProperties(
		@DefaultValue("Asia/Kolkata") ZoneId zone,
		@DefaultValue Security security,
		@DefaultValue Cors cors,
		@DefaultValue Booking booking,
		@DefaultValue Trips trips,
		@DefaultValue Journey journey,
		@DefaultValue Seed seed,
		@DefaultValue Timetable timetable) {

	public record Security(
			@DefaultValue("") String tokenSecret,
			@DefaultValue("12h") Duration tokenTtl,
			@DefaultValue("600000") int pbkdf2Iterations) {
	}

	public record Cors(@DefaultValue("http://localhost:5173") List<String> allowedOrigins) {
	}

	public record Booking(
			@DefaultValue("10m") Duration holdDuration,
			@DefaultValue("6") int maxPassengers) {
	}

	public record Trips(
			@DefaultValue("30") int horizonDays,
			@DefaultValue("3") int historyDays) {
	}

	public record Journey(
			@DefaultValue("2") int maxTransfers,
			@DefaultValue("8h") Duration maxWait,
			@DefaultValue("30") int maxResults) {
	}

	public record Seed(
			@DefaultValue("false") boolean enabled,
			@DefaultValue("Kinvia@Dev1") String password) {
	}

	/** Reference timetable (stations, train and bus services, fares) loaded from the bundled CSV files. */
	public record Timetable(@DefaultValue("true") boolean importEnabled) {
	}
}
