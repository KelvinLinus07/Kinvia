package com.smarttransit.smart_transit.notification;

import com.smarttransit.smart_transit.user.User;

/**
 * An outbound delivery route (email, SMS, push) in addition to the always-on in-app inbox. Register another
 * implementation as a Spring bean and it is used automatically.
 */
public interface NotificationChannel {

	void deliver(User recipient, NotificationType type, String title, String message);
}
