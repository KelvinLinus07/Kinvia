package com.smarttransit.smart_transit.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class CancellationPolicyTest {

	private final CancellationPolicy policy = new CancellationPolicy();

	@Test
	void moreThanADayBeforeDepartureRefundsNinetyPercent() {
		var refund = policy.refundFor(new BigDecimal("1000"), Duration.ofHours(30));
		assertEquals(90, refund.percent());
		assertEquals(new BigDecimal("900.00"), refund.amount());
	}

	@Test
	void betweenSixAndTwentyFourHoursRefundsHalf() {
		var refund = policy.refundFor(new BigDecimal("1000"), Duration.ofHours(10));
		assertEquals(50, refund.percent());
		assertEquals(new BigDecimal("500.00"), refund.amount());
	}

	@Test
	void underSixHoursRefundsNothing() {
		var refund = policy.refundFor(new BigDecimal("1000"), Duration.ofHours(2));
		assertEquals(0, refund.percent());
		assertEquals(new BigDecimal("0.00"), refund.amount());
	}

	@Test
	void operatorCancellationAlwaysRefundsInFull() {
		var refund = policy.fullRefund(new BigDecimal("742.50"));
		assertEquals(100, refund.percent());
		assertEquals(new BigDecimal("742.50"), refund.amount());
	}

	@Test
	void boundaryAtExactlyTwentyFourHoursIsTheLowerTier() {
		// The policy uses a strict "more than" comparison, so exactly 24h falls into the 50% tier.
		var refund = policy.refundFor(new BigDecimal("1000"), Duration.ofHours(24));
		assertEquals(50, refund.percent());
	}
}
