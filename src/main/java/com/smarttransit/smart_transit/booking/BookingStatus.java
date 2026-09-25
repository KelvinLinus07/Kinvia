package com.smarttransit.smart_transit.booking;

public enum BookingStatus {
	/** Payment has been started and is awaiting the provider's result. */
	PENDING,
	/** Seats are held for the traveller until the hold expires. */
	HELD,
	CONFIRMED,
	CANCELLED,
	EXPIRED,
	COMPLETED
}
