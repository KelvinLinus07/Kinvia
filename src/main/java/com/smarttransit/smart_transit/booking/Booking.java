package com.smarttransit.smart_transit.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.common.AuditedEntity;
import com.smarttransit.smart_transit.journey.Journey;
import com.smarttransit.smart_transit.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A traveller's purchase of one {@link Journey} for a party of passengers. Status changes go through the methods
 * below so illegal transitions (e.g. cancelling an expired booking) are rejected in one place.
 */
@Entity
@Table(name = "bookings",
		uniqueConstraints = @UniqueConstraint(name = "uk_bookings_reference", columnNames = "reference"),
		indexes = { @Index(name = "idx_bookings_user", columnList = "user_id, created_at"),
				@Index(name = "idx_bookings_status_expiry", columnList = "status, hold_expires_at") })
public class Booking extends AuditedEntity {

	private static final Set<BookingStatus> LIVE = Set.of(BookingStatus.HELD, BookingStatus.PENDING);

	@Column(nullable = false, length = 16)
	private String reference;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "journey_id", nullable = false, unique = true)
	private Journey journey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private BookingStatus status = BookingStatus.HELD;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal totalAmount;

	@Column(nullable = false, length = 3)
	private String currency = "INR";

	@Column(name = "hold_expires_at")
	private Instant holdExpiresAt;

	private Instant confirmedAt;

	private Instant cancelledAt;

	@Column(length = 240)
	private String cancellationReason;

	@Column(precision = 10, scale = 2)
	private BigDecimal refundAmount;

	@Column(nullable = false)
	private boolean reminderSent;

	@OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<BookingPassenger> passengers = new ArrayList<>();

	protected Booking() {
	}

	public Booking(String reference, User user, Journey journey, BigDecimal totalAmount, Instant holdExpiresAt) {
		this.reference = reference;
		this.user = user;
		this.journey = journey;
		this.totalAmount = totalAmount;
		this.holdExpiresAt = holdExpiresAt;
	}

	public BookingPassenger addPassenger(String fullName, int age, com.smarttransit.smart_transit.user.Gender gender,
			String mobile) {
		BookingPassenger passenger = new BookingPassenger(this, fullName, age, gender, mobile);
		passengers.add(passenger);
		return passenger;
	}

	public boolean isHoldExpired(Instant now) {
		return LIVE.contains(status) && holdExpiresAt != null && !holdExpiresAt.isAfter(now);
	}

	public void beginPayment(Instant now) {
		if (status != BookingStatus.HELD || isHoldExpired(now)) {
			throw ApiException.conflict(status == BookingStatus.HELD ? "Your seat hold has expired"
					: "This booking cannot be paid for (status: " + status + ")");
		}
		status = BookingStatus.PENDING;
	}

	public void paymentFailed() {
		if (status == BookingStatus.PENDING) {
			status = BookingStatus.HELD;
		}
	}

	public void confirm(Instant now) {
		if (status != BookingStatus.PENDING) {
			throw ApiException.conflict("Only a booking with payment in progress can be confirmed");
		}
		status = BookingStatus.CONFIRMED;
		confirmedAt = now;
		holdExpiresAt = null;
	}

	public void expire() {
		if (!LIVE.contains(status)) {
			throw ApiException.conflict("Only unpaid bookings can expire");
		}
		status = BookingStatus.EXPIRED;
	}

	public void cancel(Instant now, String reason, BigDecimal refund) {
		if (status == BookingStatus.CANCELLED) {
			throw ApiException.conflict("This booking is already cancelled");
		}
		if (status == BookingStatus.EXPIRED) {
			throw ApiException.conflict("This booking expired before payment and cannot be cancelled");
		}
		if (status == BookingStatus.COMPLETED) {
			throw ApiException.conflict("This journey is already completed and cannot be cancelled");
		}
		if (status == BookingStatus.PENDING) {
			throw ApiException.conflict("Payment is in progress; try again in a moment");
		}
		status = BookingStatus.CANCELLED;
		cancelledAt = now;
		cancellationReason = reason;
		refundAmount = refund;
		holdExpiresAt = null;
	}

	public void complete() {
		if (status == BookingStatus.CONFIRMED) {
			status = BookingStatus.COMPLETED;
		}
	}

	public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

	public void markReminderSent() { this.reminderSent = true; }

	public String getReference() { return reference; }

	public User getUser() { return user; }

	public Journey getJourney() { return journey; }

	public BookingStatus getStatus() { return status; }

	public BigDecimal getTotalAmount() { return totalAmount; }

	public String getCurrency() { return currency; }

	public Instant getHoldExpiresAt() { return holdExpiresAt; }

	public Instant getConfirmedAt() { return confirmedAt; }

	public Instant getCancelledAt() { return cancelledAt; }

	public String getCancellationReason() { return cancellationReason; }

	public BigDecimal getRefundAmount() { return refundAmount; }

	public boolean isReminderSent() { return reminderSent; }

	public List<BookingPassenger> getPassengers() { return passengers; }
}
