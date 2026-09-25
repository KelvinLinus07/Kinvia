package com.smarttransit.smart_transit.payment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.booking.Booking;
import com.smarttransit.smart_transit.booking.BookingRepository;
import com.smarttransit.smart_transit.booking.BookingStatus;
import com.smarttransit.smart_transit.booking.ReservationStatus;
import com.smarttransit.smart_transit.booking.SeatReservation;
import com.smarttransit.smart_transit.booking.SeatReservationRepository;
import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.notification.NotificationService;
import com.smarttransit.smart_transit.notification.NotificationType;
import com.smarttransit.smart_transit.payment.PaymentProvider.ChargeRequest;
import com.smarttransit.smart_transit.payment.PaymentProvider.ChargeResult;
import com.smarttransit.smart_transit.payment.PaymentProvider.RefundResult;
import com.smarttransit.smart_transit.ticket.TicketService;

/**
 * The transactional halves of a payment. {@link CheckoutService} calls {@link #begin} and {@link #settle} in
 * separate transactions with the gateway call in between, so no database lock is held during a network call.
 */
@Service
public class PaymentService {

	private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

	private final BookingRepository bookings;
	private final PaymentRepository payments;
	private final SeatReservationRepository reservations;
	private final PaymentProvider provider;
	private final TicketService tickets;
	private final NotificationService notifications;
	private final Clock clock;

	public PaymentService(BookingRepository bookings, PaymentRepository payments,
			SeatReservationRepository reservations, PaymentProvider provider, TicketService tickets,
			NotificationService notifications, Clock clock) {
		this.bookings = bookings;
		this.payments = payments;
		this.reservations = reservations;
		this.provider = provider;
		this.tickets = tickets;
		this.notifications = notifications;
		this.clock = clock;
	}

	/** Locks the booking, moves it to PENDING and records the attempt. */
	@Transactional
	public Payment begin(Long userId, Long bookingId, PaymentMethod method) {
		Booking booking = bookings.lockById(bookingId).filter(b -> b.getUser().getId().equals(userId))
				.orElseThrow(() -> ApiException.notFound("Booking", bookingId));
		booking.beginPayment(Instant.now(clock));
		return payments.save(new Payment(booking, provider.name(), method));
	}

	@Transactional
	public Payment settle(Long paymentId, ChargeResult result) {
		Payment payment = payments.findById(paymentId).orElseThrow();
		Booking booking = bookings.lockById(payment.getBooking().getId()).orElseThrow();
		if (!result.success()) {
			payment.failed(result.failureReason());
			booking.paymentFailed();
			notifications.send(booking.getUser(), NotificationType.PAYMENT_FAILED, "Payment failed",
					"Payment for booking " + booking.getReference() + " did not go through. Your seats are still held.",
					booking);
			return payment;
		}
		payment.succeeded(result.reference());
		if (booking.getStatus() != BookingStatus.PENDING) {
			// The hold lapsed while the gateway call was in flight: give the money back rather than sell seats twice.
			refund(booking, payment, payment.getAmount());
			return payment;
		}
		booking.confirm(Instant.now(clock));
		reservations.findByBooking(booking.getId()).forEach(SeatReservation::markBooked);
		tickets.issue(booking);
		notifications.send(booking.getUser(), NotificationType.PAYMENT_SUCCEEDED, "Payment received",
				"We received " + booking.getCurrency() + " " + payment.getAmount() + " for booking "
						+ booking.getReference() + (provider.isSimulated() ? " (development payment, no real money)." : "."),
				booking);
		notifications.send(booking.getUser(), NotificationType.BOOKING_CONFIRMED, "Booking confirmed",
				"Your journey " + booking.getJourney().getOrigin().getName() + " to "
						+ booking.getJourney().getDestination().getName() + " is confirmed. Your ticket is ready.",
				booking);
		return payment;
	}

	/** Refunds part or all of the successful payment for a booking, if there is one. */
	@Transactional
	public RefundStatus refundFor(Booking booking, BigDecimal amount) {
		Payment payment = payments.findFirstByBookingIdAndStatus(booking.getId(), PaymentStatus.SUCCEEDED).orElse(null);
		if (payment == null || amount.signum() <= 0) {
			return RefundStatus.NONE;
		}
		return refund(booking, payment, amount);
	}

	private RefundStatus refund(Booking booking, Payment payment, BigDecimal amount) {
		RefundResult result;
		try {
			result = provider.refund(new PaymentProvider.RefundRequest(payment.getProviderReference(), amount,
					payment.getCurrency()));
		}
		catch (RuntimeException ex) {
			log.error("Refund call failed for booking {}", booking.getReference(), ex);
			result = new RefundResult(false, null, "Gateway error");
		}
		if (result.success()) {
			payment.refunded(amount, result.reference());
			notifications.send(booking.getUser(), NotificationType.REFUND_ISSUED, "Refund issued",
					"A refund of " + payment.getCurrency() + " " + amount + " for booking " + booking.getReference()
							+ " has been issued.", booking);
			return RefundStatus.REFUNDED;
		}
		payment.refundFailed(amount, result.failureReason());
		log.error("Refund of {} for booking {} needs manual attention: {}", amount, booking.getReference(),
				result.failureReason());
		return RefundStatus.FAILED;
	}

	public boolean isSimulated() {
		return provider.isSimulated();
	}

	public String providerName() {
		return provider.name();
	}

	public List<Payment> paymentsSince(Instant since) {
		return payments.findSucceededSince(since);
	}

	ChargeRequest chargeRequest(Payment payment, String reference, String instrument) {
		return new ChargeRequest(reference, payment.getAmount(), payment.getCurrency(), payment.getMethod(), instrument);
	}

	ChargeResult charge(ChargeRequest request) {
		return provider.charge(request);
	}
}
