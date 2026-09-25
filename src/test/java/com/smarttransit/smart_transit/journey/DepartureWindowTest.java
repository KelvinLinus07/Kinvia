package com.smarttransit.smart_transit.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class DepartureWindowTest {

	@Test
	void bothEndsAreInclusive() {
		DepartureWindow window = new DepartureWindow(LocalTime.of(6, 0), LocalTime.of(12, 0));

		assertTrue(window.contains(LocalTime.of(6, 0)));
		assertTrue(window.contains(LocalTime.of(12, 0)));
		assertFalse(window.contains(LocalTime.of(5, 59)));
		assertFalse(window.contains(LocalTime.of(12, 1)));
	}

	@Test
	void secondsAreIgnored() {
		DepartureWindow window = new DepartureWindow(LocalTime.of(6, 0), LocalTime.of(12, 0));

		assertTrue(window.contains(LocalTime.of(12, 0, 45)));
	}

	@Test
	void aRangeThatStartsAfterItEndsWrapsPastMidnight() {
		DepartureWindow overnight = new DepartureWindow(LocalTime.of(22, 0), LocalTime.of(2, 0));

		assertTrue(overnight.contains(LocalTime.of(23, 30)));
		assertTrue(overnight.contains(LocalTime.of(0, 15)));
		assertTrue(overnight.contains(LocalTime.of(2, 0)));
		assertFalse(overnight.contains(LocalTime.of(2, 1)));
		assertFalse(overnight.contains(LocalTime.of(12, 0)));
	}

	@Test
	void everyMinuteOfTheDayBelongsToExactlyOneNamedBand() {
		for (int minute = 0; minute < 24 * 60; minute++) {
			LocalTime time = LocalTime.of(minute / 60, minute % 60);
			long bands = java.util.Arrays.stream(TimeOfDay.values()).filter(b -> b.window().contains(time)).count();
			assertEquals(1, bands, "minute " + time + " should be in exactly one band");
		}
	}

	@Test
	void namedBandsMatchTheAdvertisedHours() {
		assertTrue(TimeOfDay.NIGHT.window().contains(LocalTime.MIDNIGHT));
		assertTrue(TimeOfDay.MORNING.window().contains(LocalTime.of(6, 0)));
		assertTrue(TimeOfDay.AFTERNOON.window().contains(LocalTime.of(12, 0)));
		assertTrue(TimeOfDay.EVENING.window().contains(LocalTime.of(23, 59)));
	}
}
