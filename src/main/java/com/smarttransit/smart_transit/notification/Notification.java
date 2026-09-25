package com.smarttransit.smart_transit.notification;

import com.smarttransit.smart_transit.booking.Booking;
import com.smarttransit.smart_transit.common.AuditedEntity;
import com.smarttransit.smart_transit.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notifications_user", columnList = "user_id, created_at"))
public class Notification extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private NotificationType type;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(nullable = false, length = 600)
	private String message;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "booking_id")
	private Booking booking;

	@Column(name = "is_read", nullable = false)
	private boolean read;

	protected Notification() {
	}

	public Notification(User user, NotificationType type, String title, String message, Booking booking) {
		this.user = user;
		this.type = type;
		this.title = title;
		this.message = message;
		this.booking = booking;
	}

	public void markRead() { this.read = true; }

	public User getUser() { return user; }

	public NotificationType getType() { return type; }

	public String getTitle() { return title; }

	public String getMessage() { return message; }

	public Booking getBooking() { return booking; }

	public boolean isRead() { return read; }
}
