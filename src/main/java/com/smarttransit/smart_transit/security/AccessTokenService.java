package com.smarttransit.smart_transit.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.OptionalLong;

import org.springframework.stereotype.Service;

import com.smarttransit.smart_transit.config.KinviaProperties;

/**
 * Issues and verifies stateless access tokens of the form {@code base64url(v1:userId:expiryEpochSecond).signature}.
 * The role is deliberately not embedded: it is read from the database on each request so role changes and
 * account disabling take effect immediately.
 */
@Service
public class AccessTokenService {

	private static final String VERSION = "v1";

	private final HmacSigner signer;
	private final Clock clock;
	private final Duration ttl;

	public AccessTokenService(HmacSigner signer, Clock clock, KinviaProperties properties) {
		this.signer = signer;
		this.clock = clock;
		this.ttl = properties.security().tokenTtl();
	}

	public String issue(long userId) {
		long expiry = Instant.now(clock).plus(ttl).getEpochSecond();
		String payload = Base64.getUrlEncoder().withoutPadding()
				.encodeToString((VERSION + ":" + userId + ":" + expiry).getBytes(StandardCharsets.UTF_8));
		return payload + "." + signer.sign(payload);
	}

	public Duration ttl() {
		return ttl;
	}

	public OptionalLong verify(String token) {
		int dot = token == null ? -1 : token.indexOf('.');
		if (dot < 1 || !signer.verify(token.substring(0, dot), token.substring(dot + 1))) {
			return OptionalLong.empty();
		}
		try {
			String[] parts = new String(Base64.getUrlDecoder().decode(token.substring(0, dot)), StandardCharsets.UTF_8)
					.split(":");
			boolean valid = parts.length == 3 && VERSION.equals(parts[0])
					&& Long.parseLong(parts[2]) > Instant.now(clock).getEpochSecond();
			return valid ? OptionalLong.of(Long.parseLong(parts[1])) : OptionalLong.empty();
		}
		catch (IllegalArgumentException ex) {
			return OptionalLong.empty();
		}
	}
}
