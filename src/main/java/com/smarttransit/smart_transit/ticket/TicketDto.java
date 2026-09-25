package com.smarttransit.smart_transit.ticket;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.smarttransit.smart_transit.booking.BookingDtos.SegmentSummary;
import com.smarttransit.smart_transit.booking.BookingDtos.StationRef;

/** Everything needed to render a ticket, independent of how it is displayed (web, PDF). */
public record TicketDto(String ticketNumber, String bookingReference, TicketStatus status, Instant issuedAt,
		StationRef origin, StationRef destination, String passengerName, BigDecimal totalAmount, String currency,
		List<SegmentSummary> segments, List<PassengerLine> passengers, String qrPayload, String qrSvgDataUri) {

	public record PassengerLine(String fullName, int age, String gender, List<SeatLine> seats) {
	}

	public record SeatLine(int segmentIndex, String seatLabel, String coachCode, String classLabel) {
	}
}
