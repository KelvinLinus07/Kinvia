package com.smarttransit.smart_transit.booking;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.notification.NotificationService;
import com.smarttransit.smart_transit.notification.NotificationType;

/**
 * Background upkeep for time-based state: releasing lapsed seat holds, expiring unpaid bookings, marking finished
 * journeys complete, and sending reminders. Each run is independent and safe to overlap or retry.
 */
@Component
public class BookingMaintenanceJobs {

	private static final Logger log = LoggerFactory.getLogger(BookingMaintenanceJobs.class);
	private static final Duration PENDING_GRACE = Duration.ofMinutes(5);
	private static final Duration REMINDER_WINDOW = Duration.ofHours(6);

	private final BookingRepository bookings;
	private final SeatReservationRepository reservations;
	private final NotificationService notifications;
	private final Clock clock;

	public BookingMaintenanceJobs(BookingRepository bookings, SeatReservationRepository reservations,
			NotificationService notifications, Clock clock) {
		this.bookings = bookings;
		this.reservations = reservations;
		this.notifications = notifications;
		this.clock = clock;
	}

	@Scheduled(fixedDelay = 60_000)
	@Transactional
	public void releaseExpiredHolds() {
		int expired = reservations.expireLooseHolds(Instant.now(clock));
		if (expired > 0) {
			log.debug("Expired {} unattached seat holds", expired);
		}
	}

	@Scheduled(fixedDelay = 60_000)
	@Transactional
	public void expireUnpaidBookings() {
		Instant now = Instant.now(clock);
		List<Booking> expirable = bookings.findExpirable(now, now.minus(PENDING_GRACE));
		for (Booking booking : expirable) {
			booking.expire();
			reservations.findByBooking(booking.getId()).forEach(SeatReservation::release);
			notifications.send(booking.getUser(), NotificationType.BOOKING_EXPIRED, "Booking expired",
					"Booking " + booking.getReference() + " expired because payment was not completed in time.",
					booking);
		}
		if (!expirable.isEmpty()) {
			log.info("Expired {} unpaid bookings", expirable.size());
		}
	}

	@Scheduled(fixedDelay = 300_000)
	@Transactional
	public void completeFinishedJourneys() {
		bookings.findFinished(LocalDateTime.now(clock)).forEach(Booking::complete);
	}

	@Scheduled(fixedDelay = 900_000)
	@Transactional
	public void sendUpcomingJourneyReminders() {
		LocalDateTime now = LocalDateTime.now(clock);
		List<Booking> due = bookings.findNeedingReminder(now, now.plus(REMINDER_WINDOW));
		for (Booking booking : due) {
			booking.markReminderSent();
			notifications.send(booking.getUser(), NotificationType.JOURNEY_REMINDER, "Your journey is coming up",
					"Your journey from " + booking.getJourney().getOrigin().getName() + " to "
							+ booking.getJourney().getDestination().getName() + " departs "
							+ booking.getJourney().getDeparture() + ".",
					booking);
		}
	}
}
