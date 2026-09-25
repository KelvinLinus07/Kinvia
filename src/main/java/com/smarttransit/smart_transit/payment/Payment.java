package com.smarttransit.smart_transit.payment;

import java.math.BigDecimal;

import com.smarttransit.smart_transit.booking.Booking;
import com.smarttransit.smart_transit.common.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One attempt to pay for a booking. Payment instruments (card numbers etc.) are never stored. */
@Entity
@Table(name = "payments", indexes = @Index(name = "idx_payments_booking", columnList = "booking_id"))
public class Payment extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_id", nullable = false)
	private Booking booking;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(nullable = false, length = 24)
	private String provider;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private PaymentMethod method;

	@Column(length = 64)
	private String providerReference;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private PaymentStatus status = PaymentStatus.INITIATED;

	@Column(length = 240)
	private String failureReason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private RefundStatus refundStatus = RefundStatus.NONE;

	@Column(precision = 10, scale = 2)
	private BigDecimal refundAmount;

	@Column(length = 64)
	private String refundReference;

	protected Payment() {
	}

	public Payment(Booking booking, String provider, PaymentMethod method) {
		this.booking = booking;
		this.amount = booking.getTotalAmount();
		this.currency = booking.getCurrency();
		this.provider = provider;
		this.method = method;
	}

	public void succeeded(String providerReference) {
		this.status = PaymentStatus.SUCCEEDED;
		this.providerReference = providerReference;
	}

	public void failed(String reason) {
		this.status = PaymentStatus.FAILED;
		this.failureReason = reason;
	}

	public void refunded(BigDecimal amount, String reference) {
		this.refundStatus = RefundStatus.REFUNDED;
		this.refundAmount = amount;
		this.refundReference = reference;
	}

	public void refundFailed(BigDecimal amount, String reason) {
		this.refundStatus = RefundStatus.FAILED;
		this.refundAmount = amount;
		this.failureReason = reason;
	}

	public Booking getBooking() { return booking; }

	public BigDecimal getAmount() { return amount; }

	public String getCurrency() { return currency; }

	public String getProvider() { return provider; }

	public PaymentMethod getMethod() { return method; }

	public String getProviderReference() { return providerReference; }

	public PaymentStatus getStatus() { return status; }

	public String getFailureReason() { return failureReason; }

	public RefundStatus getRefundStatus() { return refundStatus; }

	public BigDecimal getRefundAmount() { return refundAmount; }

	public String getRefundReference() { return refundReference; }
}
