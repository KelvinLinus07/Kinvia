package com.smarttransit.smart_transit.notification;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smarttransit.smart_transit.common.PageResponse;
import com.smarttransit.smart_transit.security.CurrentUser;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notifications;

	public NotificationController(NotificationService notifications) {
		this.notifications = notifications;
	}

	@GetMapping
	PageResponse<NotificationDto> mine(CurrentUser user, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return notifications.mine(user.id(), page, size);
	}

	@GetMapping("/unread-count")
	Map<String, Long> unreadCount(CurrentUser user) {
		return Map.of("count", notifications.unreadCount(user.id()));
	}

	@PatchMapping("/{id}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void markRead(CurrentUser user, @PathVariable Long id) {
		notifications.markRead(user.id(), id);
	}

	@PostMapping("/read-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void markAllRead(CurrentUser user) {
		notifications.markAllRead(user.id());
	}
}
