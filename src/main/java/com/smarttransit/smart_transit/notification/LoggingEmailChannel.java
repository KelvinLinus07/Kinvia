package com.smarttransit.smart_transit.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.smarttransit.smart_transit.user.User;

/** Development stand-in for an email provider: records what would have been sent. Sends nothing. */
@Component
public class LoggingEmailChannel implements NotificationChannel {

	private static final Logger log = LoggerFactory.getLogger(LoggingEmailChannel.class);

	@Override
	public void deliver(User recipient, NotificationType type, String title, String message) {
		log.info("[DEV EMAIL - not sent] to={} type={} subject=\"{}\"", recipient.getEmail(), type, title);
	}
}
