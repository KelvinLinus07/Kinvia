package com.smarttransit.smart_transit.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class BookingReferencesTest {

	private static final Pattern FORMAT = Pattern.compile("^KV-[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{8}$");

	@Test
	void generatesReferencesInTheExpectedFormat() {
		for (int i = 0; i < 200; i++) {
			assertTrue(FORMAT.matcher(BookingReferences.next()).matches());
		}
	}

	@Test
	void doesNotUseLookAlikeCharacters() {
		String reference = BookingReferences.next();
		for (char c : new char[] { 'I', 'O', '0', '1' }) {
			assertTrue(reference.indexOf(c) < 0, "reference should not contain the look-alike character '" + c + "'");
		}
	}

	@Test
	void generatesDistinctReferencesInPractice() {
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 500; i++) {
			seen.add(BookingReferences.next());
		}
		assertEquals(500, seen.size());
	}
}
