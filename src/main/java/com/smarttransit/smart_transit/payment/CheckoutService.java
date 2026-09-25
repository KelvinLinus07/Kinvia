package com.smarttransit.smart_transit.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.smarttransit.smart_transit.booking.BookingDtos.BookingDto;
import com.smarttransit.smart_transit.booking.BookingService;
import com.smarttransit.smart_transit.booking.BookingStatus;
import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.payment.PaymentProvider.ChargeResult;
import com.smarttransit.smart_transit.security.CurrentUser;

/**
 * Orchestrates a payment: begin (transaction) -> gateway call (no transaction) -> settle (transaction). It is
 * deliberately not transactional itself.
 */
@Service
public class CheckoutService {

	private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

	private final PaymentService payments;
	private final BookingService bookings;

	public CheckoutService(PaymentService payments, BookingService bookings) {
		this.payments = payments;
		this.bookings = bookings;
	}

	public BookingDto pay(CurrentUser user, Long bookingId, PaymentMethod method, String instrument) {
		Payment payment = payments.begin(user.id(), bookingId, method);
		ChargeResult result;
		try {
			result = payments.charge(payments.chargeRequest(payment, payment.getBooking().getReference(), instrument));
		}
		catch (RuntimeException ex) {
			log.error("Payment gateway call failed for booking {}", bookingId, ex);
			result = ChargeResult.declined("The payment service is unavailable. Please try again.");
		}
		Payment settled = payments.settle(payment.getId(), result);
		if (settled.getStatus() == PaymentStatus.FAILED) {
			throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Payment failed: " + settled.getFailureReason());
		}
		BookingDto booking = bookings.get(user, bookingId);
		if (booking.status() != BookingStatus.CONFIRMED) {
			throw ApiException.conflict("Your seat hold expired during payment; the amount has been refunded");
		}
		return booking;
	}
}
