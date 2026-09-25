package com.smarttransit.smart_transit.ticket;

import java.time.Instant;

import com.smarttransit.smart_transit.booking.Booking;
import com.smarttransit.smart_transit.common.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** The digital ticket for a confirmed booking. One ticket covers every segment and passenger. */
@Entity
@Table(name = "tickets", uniqueConstraints = {
		@UniqueConstraint(name = "uk_tickets_number", columnNames = "ticket_number"),
		@UniqueConstraint(name = "uk_tickets_booking", columnNames = "booking_id") })
public class Ticket extends AuditedEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_id", nullable = false)
	private Booking booking;

	@Column(name = "ticket_number", nullable = false, length = 20)
	private String ticketNumber;

	/** Signed content of the QR code; see {@link TicketService#verify}. */
	@Column(nullable = false, length = 120)
	private String qrPayload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TicketStatus status = TicketStatus.ACTIVE;

	@Column(nullable = false)
	private Instant issuedAt;

	protected Ticket() {
	}

	public Ticket(Booking booking, String ticketNumber, String qrPayload, Instant issuedAt) {
		this.booking = booking;
		this.ticketNumber = ticketNumber;
		this.qrPayload = qrPayload;
		this.issuedAt = issuedAt;
	}

	public void cancel() { this.status = TicketStatus.CANCELLED; }

	public Booking getBooking() { return booking; }

	public String getTicketNumber() { return ticketNumber; }

	public String getQrPayload() { return qrPayload; }

	public TicketStatus getStatus() { return status; }

	public Instant getIssuedAt() { return issuedAt; }
}
