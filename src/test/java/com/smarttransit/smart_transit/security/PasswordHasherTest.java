package com.smarttransit.smart_transit.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.smarttransit.smart_transit.testsupport.TestProperties;

class PasswordHasherTest {

	// Low iteration count keeps the test fast; production uses the much higher configured default.
	private final PasswordHasher hasher = new PasswordHasher(TestProperties.withSecurity("irrelevant-for-this-test-xxxxxxxxxxxxxxx", 1000));

	@Test
	void aPasswordMatchesItsOwnHash() {
		String hash = hasher.hash("correct horse battery staple");
		assertTrue(hasher.matches("correct horse battery staple", hash));
	}

	@Test
	void aDifferentPasswordDoesNotMatch() {
		String hash = hasher.hash("correct horse battery staple");
		assertFalse(hasher.matches("wrong password", hash));
	}

	@Test
	void hashingTheSamePasswordTwiceProducesDifferentHashes() {
		String first = hasher.hash("same password");
		String second = hasher.hash("same password");
		assertNotEquals(first, second, "each hash should use a fresh random salt");
	}

	@Test
	void malformedStoredHashesAreRejectedRatherThanThrowing() {
		assertFalse(hasher.matches("anything", "not-a-real-hash"));
		assertFalse(hasher.matches("anything", null));
	}
}
