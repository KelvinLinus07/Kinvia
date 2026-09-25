package com.smarttransit.smart_transit.payment;

import java.math.BigDecimal;

/**
 * Boundary to a payment gateway. To go live, implement this interface for Razorpay, Stripe or similar, register it
 * as a bean, and set {@code kinvia.payment.provider} accordingly. Nothing else in the booking flow changes.
 */
public interface PaymentProvider {

	/** Stable identifier stored with each payment, e.g. "DEVELOPMENT" or "RAZORPAY". */
	String name();

	/** True when no real money moves; surfaced to the UI so test payments are never mistaken for real ones. */
	boolean isSimulated();

	ChargeResult charge(ChargeRequest request);

	RefundResult refund(RefundRequest request);

	record ChargeRequest(String bookingReference, BigDecimal amount, String currency, PaymentMethod method,
			String instrument) {
	}

	record ChargeResult(boolean success, String reference, String failureReason) {

		public static ChargeResult approved(String reference) {
			return new ChargeResult(true, reference, null);
		}

		public static ChargeResult declined(String reason) {
			return new ChargeResult(false, null, reason);
		}
	}

	record RefundRequest(String originalReference, BigDecimal amount, String currency) {
	}

	record RefundResult(boolean success, String reference, String failureReason) {
	}
}
