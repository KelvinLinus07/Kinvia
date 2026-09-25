package com.smarttransit.smart_transit.booking;

import java.security.SecureRandom;

/** Human-friendly booking references such as KV-7QX2M9TB (no look-alike characters). */
final class BookingReferences {

	private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
	private static final SecureRandom RANDOM = new SecureRandom();

	private BookingReferences() {
	}

	static String next() {
		StringBuilder reference = new StringBuilder("KV-");
		for (int i = 0; i < 8; i++) {
			reference.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return reference.toString();
	}
}
