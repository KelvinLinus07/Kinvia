package com.smarttransit.smart_transit.booking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

import org.springframework.stereotype.Component;

/**
 * Refund tiers by time left before the first departure: more than 24h before, 90% back; more than 6h, 50%; closer
 * than that, nothing. Whole-booking cancellations only.
 */
@Component
public class CancellationPolicy {

	private static final Duration FULL_TIER = Duration.ofHours(24);
	private static final Duration PARTIAL_TIER = Duration.ofHours(6);

	public record Refund(int percent, BigDecimal amount, String explanation) {
	}

	public Refund refundFor(BigDecimal amountPaid, Duration untilDeparture) {
		int percent = untilDeparture.compareTo(FULL_TIER) > 0 ? 90 : untilDeparture.compareTo(PARTIAL_TIER) > 0 ? 50 : 0;
		BigDecimal amount = amountPaid.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), 2,
				RoundingMode.HALF_UP);
		String explanation = switch (percent) {
			case 90 -> "More than 24 hours before departure: 90% is refunded.";
			case 50 -> "Between 6 and 24 hours before departure: 50% is refunded.";
			default -> "Less than 6 hours before departure: no refund applies.";
		};
		return new Refund(percent, amount, explanation);
	}

	/** Used when the operator cancels the service: the traveller always gets everything back. */
	public Refund fullRefund(BigDecimal amountPaid) {
		return new Refund(100, amountPaid, "The operator cancelled this service: full refund.");
	}
}
