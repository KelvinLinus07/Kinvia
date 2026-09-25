package com.smarttransit.smart_transit.testsupport;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import com.smarttransit.smart_transit.config.KinviaProperties;

/** Builds a fully-populated {@link KinviaProperties} for tests, without needing a Spring context. */
public final class TestProperties {

	private TestProperties() {
	}

	public static KinviaProperties defaults() {
		return withSecurity("test-only-secret-at-least-32-characters-long", 1000);
	}

	public static KinviaProperties withSecurity(String tokenSecret, int pbkdf2Iterations) {
		return new KinviaProperties(ZoneId.of("Asia/Kolkata"),
				new KinviaProperties.Security(tokenSecret, Duration.ofHours(12), pbkdf2Iterations),
				new KinviaProperties.Cors(List.of("http://localhost:5173")),
				new KinviaProperties.Booking(Duration.ofMinutes(10), 6), new KinviaProperties.Trips(30, 3),
				new KinviaProperties.Journey(2, Duration.ofHours(8), 30), new KinviaProperties.Seed(false, "Test@1234"),
				new KinviaProperties.Timetable(false));
	}
}
