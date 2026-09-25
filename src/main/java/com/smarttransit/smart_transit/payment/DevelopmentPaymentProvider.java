package com.smarttransit.smart_transit.payment;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Simulated gateway for local development. It moves no money and inspects the instrument only to let developers
 * exercise failures: a card number ending in 0002, or a UPI id / bank name containing "fail", is declined.
 */
@Component
@ConditionalOnProperty(name = "kinvia.payment.provider", havingValue = "development", matchIfMissing = true)
public class DevelopmentPaymentProvider implements PaymentProvider {

	@Override
	public String name() {
		return "DEVELOPMENT";
	}

	@Override
	public boolean isSimulated() {
		return true;
	}

	@Override
	public ChargeResult charge(ChargeRequest request) {
		String instrument = request.instrument() == null ? "" : request.instrument().replaceAll("\\s+", "").toLowerCase();
		if (instrument.isEmpty()) {
			return ChargeResult.declined("No payment instrument supplied");
		}
		if (instrument.endsWith("0002") || instrument.contains("fail")) {
			return ChargeResult.declined("Declined by the development gateway (test failure trigger)");
		}
		return ChargeResult.approved("DEV-PAY-" + shortId());
	}

	@Override
	public RefundResult refund(RefundRequest request) {
		return new RefundResult(true, "DEV-REF-" + shortId(), null);
	}

	private static String shortId() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
	}
}
