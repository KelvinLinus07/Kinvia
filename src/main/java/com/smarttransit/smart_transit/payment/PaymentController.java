package com.smarttransit.smart_transit.payment;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smarttransit.smart_transit.booking.BookingDtos.BookingDto;
import com.smarttransit.smart_transit.payment.PaymentDtos.PaymentRequest;
import com.smarttransit.smart_transit.security.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings/{bookingId}/payment")
public class PaymentController {

	private final CheckoutService checkout;

	public PaymentController(CheckoutService checkout) {
		this.checkout = checkout;
	}

	@PostMapping
	BookingDto pay(CurrentUser user, @org.springframework.web.bind.annotation.PathVariable Long bookingId,
			@Valid @RequestBody PaymentRequest request) {
		return checkout.pay(user, bookingId, request.method(), request.instrument());
	}
}
