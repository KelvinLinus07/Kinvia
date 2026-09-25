package com.smarttransit.smart_transit.schedule;

public enum TripStatus {
	SCHEDULED, DELAYED, DEPARTED, COMPLETED, CANCELLED;

	public boolean isBookable() {
		return this == SCHEDULED || this == DELAYED;
	}
}
