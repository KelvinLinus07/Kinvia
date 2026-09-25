package com.smarttransit.smart_transit.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.smarttransit.smart_transit.config.KinviaProperties;

/** HMAC-SHA256 signing shared by access tokens and ticket QR codes. */
@Component
public class HmacSigner {

	private static final Logger log = LoggerFactory.getLogger(HmacSigner.class);
	private static final String ALGORITHM = "HmacSHA256";

	private final byte[] secret;

	public HmacSigner(KinviaProperties properties) {
		this.secret = resolveSecret(properties.security().tokenSecret());
	}

	private static byte[] resolveSecret(String configured) {
		if (configured != null && configured.length() >= 32) {
			return configured.getBytes(StandardCharsets.UTF_8);
		}
		log.warn("KINVIA_TOKEN_SECRET is not set (or shorter than 32 characters); using a random secret. "
				+ "Sessions and QR codes will be invalidated on restart.");
		byte[] random = new byte[48];
		new SecureRandom().nextBytes(random);
		return random;
	}

	/** Returns the URL-safe Base64 signature of {@code data}. */
	public String sign(String data) {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(secret, ALGORITHM));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("HMAC signing failed", ex);
		}
	}

	public boolean verify(String data, String signature) {
		if (signature == null) {
			return false;
		}
		return MessageDigest.isEqual(sign(data).getBytes(StandardCharsets.UTF_8),
				signature.getBytes(StandardCharsets.UTF_8));
	}
}
