package com.smarttransit.smart_transit.notification;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.booking.Booking;
import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.common.PageResponse;
import com.smarttransit.smart_transit.user.Role;
import com.smarttransit.smart_transit.user.User;
import com.smarttransit.smart_transit.user.UserRepository;

@Service
public class NotificationService {

	private final NotificationRepository notifications;
	private final UserRepository users;
	private final List<NotificationChannel> channels;

	public NotificationService(NotificationRepository notifications, UserRepository users,
			List<NotificationChannel> channels) {
		this.notifications = notifications;
		this.users = users;
		this.channels = channels;
	}

	/** Stores the in-app notification and fans it out to every outbound channel. */
	@Transactional
	public void send(User user, NotificationType type, String title, String message, Booking booking) {
		notifications.save(new Notification(user, type, title, message, booking));
		channels.forEach(channel -> channel.deliver(user, type, title, message));
	}

	@Transactional
	public void sendToAll(List<User> recipients, NotificationType type, String title, String message) {
		recipients.forEach(user -> send(user, type, title, message, null));
	}

	/** Alerts every enabled passenger (used for network-wide service alerts). */
	@Transactional
	public int broadcastToPassengers(NotificationType type, String title, String message) {
		List<User> passengers = users.findAll().stream().filter(u -> u.getRole() == Role.PASSENGER && u.isEnabled())
				.toList();
		sendToAll(passengers, type, title, message);
		return passengers.size();
	}

	@Transactional(readOnly = true)
	public PageResponse<NotificationDto> mine(Long userId, int page, int size) {
		return PageResponse.of(notifications.findByUserIdOrderByCreatedAtDesc(userId, pageable(page, size)),
				NotificationDto::from);
	}

	@Transactional(readOnly = true)
	public PageResponse<NotificationDto> recent(int page, int size) {
		return PageResponse.of(notifications.findAllByOrderByCreatedAtDesc(pageable(page, size)), NotificationDto::from);
	}

	@Transactional(readOnly = true)
	public long unreadCount(Long userId) {
		return notifications.countByUserIdAndReadFalse(userId);
	}

	@Transactional
	public void markRead(Long userId, Long id) {
		notifications.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Notification", id))
				.markRead();
	}

	@Transactional
	public void markAllRead(Long userId) {
		notifications.markAllRead(userId);
	}

	private static Pageable pageable(int page, int size) {
		return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
	}
}
