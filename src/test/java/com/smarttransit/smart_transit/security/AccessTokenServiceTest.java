package com.smarttransit.smart_transit.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

import com.smarttransit.smart_transit.testsupport.TestProperties;

class AccessTokenServiceTest {

	private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
	private final HmacSigner signer = new HmacSigner(TestProperties.defaults());
	private final AccessTokenService tokens = new AccessTokenService(signer, clock, TestProperties.defaults());

	@Test
	void aFreshlyIssuedTokenVerifiesToTheSameUserId() {
		String token = tokens.issue(42L);
		OptionalLong verified = tokens.verify(token);
		assertTrue(verified.isPresent());
		assertEquals(42L, verified.getAsLong());
	}

	@Test
	void anExpiredTokenDoesNotVerify() {
		Clock past = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
		AccessTokenService longAgo = new AccessTokenService(signer, past, TestProperties.defaults());
		String token = longAgo.issue(7L);

		assertTrue(tokens.verify(token).isEmpty(), "a token issued in 2020 with a 12h TTL must not verify in 2026");
	}

	@Test
	void aTamperedTokenDoesNotVerify() {
		String token = tokens.issue(1L);
		String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");
		assertTrue(tokens.verify(tampered).isEmpty());
	}

	@Test
	void garbageInputNeverThrows() {
		assertTrue(tokens.verify(null).isEmpty());
		assertTrue(tokens.verify("").isEmpty());
		assertTrue(tokens.verify("not-a-token-at-all").isEmpty());
	}
}
