package com.smarttransit.smart_transit.notification;

import java.time.Instant;

public record NotificationDto(Long id, NotificationType type, String title, String message, boolean read,
		Instant createdAt, Long bookingId, String bookingReference, String recipient) {

	public static NotificationDto from(Notification n) {
		return new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.isRead(),
				n.getCreatedAt(), n.getBooking() == null ? null : n.getBooking().getId(),
				n.getBooking() == null ? null : n.getBooking().getReference(), n.getUser().getEmail());
	}
}
