package com.smarttransit.smart_transit.journey;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * A time-of-day range for the departure from the traveller's origin. Both ends are inclusive to the minute. A
 * range whose start is later than its end (for example 22:00-02:00) wraps past midnight.
 */
public record DepartureWindow(LocalTime from, LocalTime to) {

	public DepartureWindow {
		from = from.truncatedTo(ChronoUnit.MINUTES);
		to = to.truncatedTo(ChronoUnit.MINUTES);
	}

	public boolean contains(LocalTime time) {
		LocalTime minute = time.truncatedTo(ChronoUnit.MINUTES);
		if (from.isAfter(to)) {
			return !minute.isBefore(from) || !minute.isAfter(to);
		}
		return !minute.isBefore(from) && !minute.isAfter(to);
	}
}
