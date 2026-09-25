package com.smarttransit.smart_transit.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.payment.PaymentMethod;
import com.smarttransit.smart_transit.payment.PaymentStatus;
import com.smarttransit.smart_transit.payment.RefundStatus;
import com.smarttransit.smart_transit.schedule.TripStatus;
import com.smarttransit.smart_transit.user.Gender;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request and response shapes for bookings. */
public interface BookingDtos {

	record PassengerInput(
			@NotBlank @Size(max = 120) String fullName,
			@Min(1) @Max(120) int age,
			@NotNull Gender gender,
			@Size(max = 20) String mobile) {
	}

	/** Assigns a held seat to a passenger for one segment of the journey. */
	record SeatAssignment(@Min(0) int segmentIndex, @Min(0) int passengerIndex, @NotNull Long holdId) {
	}

	record CreateBookingRequest(
			@NotBlank String journeyKey,
			@NotEmpty @Valid List<PassengerInput> passengers,
			@NotEmpty @Valid List<SeatAssignment> seats) {
	}

	record PaymentRequest(@NotNull PaymentMethod method, @NotBlank @Size(max = 64) String instrument) {
	}

	record StationRef(Long id, String code, String name, String city) {
	}

	record SegmentSummary(int order, Long tripId, TransportMode mode, String routeName, String vehicleName,
			String vehicleNumber, String operatorName, StationRef board, StationRef alight,
			LocalDateTime departure, LocalDateTime arrival, BigDecimal fare, TripStatus tripStatus,
			int delayMinutes) {
	}

	record SeatSummary(int segmentIndex, String seatLabel, String coachCode, String classLabel, BigDecimal fare) {
	}

	record PassengerSummary(Long id, String fullName, int age, Gender gender, String mobile, List<SeatSummary> seats) {
	}

	record PaymentSummary(Long id, PaymentStatus status, PaymentMethod method, String provider, boolean simulated,
			BigDecimal amount, String reference, RefundStatus refundStatus, BigDecimal refundAmount,
			String failureReason, Instant createdAt) {
	}

	record BookingSummaryDto(Long id, String reference, BookingStatus status, BigDecimal totalAmount, String currency,
			Instant holdExpiresAt, Instant createdAt, StationRef origin, StationRef destination,
			LocalDateTime departure, LocalDateTime arrival, int transfers, List<TransportMode> modes,
			int passengerCount, boolean cancellable) {
	}

	record BookingDto(BookingSummaryDto summary, List<SegmentSummary> segments, List<PassengerSummary> passengers,
			PaymentSummary payment, String ticketNumber, Instant confirmedAt, Instant cancelledAt,
			String cancellationReason, BigDecimal refundAmount) {

		public Long id() { return summary.id(); }

		public BookingStatus status() { return summary.status(); }
	}

	record CancellationPreview(boolean cancellable, int refundPercent, BigDecimal refundAmount, String message) {
	}
}
