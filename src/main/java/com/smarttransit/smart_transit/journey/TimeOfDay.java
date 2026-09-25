package com.smarttransit.smart_transit.journey;

import java.time.LocalTime;

/** Named departure-time bands offered in the search form. Both ends are inclusive, to the minute. */
public enum TimeOfDay {
	NIGHT(LocalTime.of(0, 0), LocalTime.of(5, 59)),
	MORNING(LocalTime.of(6, 0), LocalTime.of(11, 59)),
	AFTERNOON(LocalTime.of(12, 0), LocalTime.of(17, 59)),
	EVENING(LocalTime.of(18, 0), LocalTime.of(23, 59));

	private final DepartureWindow window;

	TimeOfDay(LocalTime from, LocalTime to) {
		this.window = new DepartureWindow(from, to);
	}

	public DepartureWindow window() {
		return window;
	}
}
