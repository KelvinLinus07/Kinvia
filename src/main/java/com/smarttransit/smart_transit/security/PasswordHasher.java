package com.smarttransit.smart_transit.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Component;

import com.smarttransit.smart_transit.config.KinviaProperties;

/**
 * PBKDF2-HMAC-SHA256 with a per-password random salt. The iteration count is stored with each hash so it can be
 * raised later without invalidating existing passwords.
 */
@Component
public class PasswordHasher {

	private static final String PREFIX = "pbkdf2_sha256";
	private static final int SALT_BYTES = 16;
	private static final int KEY_BITS = 256;

	private final int iterations;
	private final SecureRandom random = new SecureRandom();

	public PasswordHasher(KinviaProperties properties) {
		this.iterations = properties.security().pbkdf2Iterations();
	}

	public String hash(String rawPassword) {
		byte[] salt = new byte[SALT_BYTES];
		random.nextBytes(salt);
		byte[] key = derive(rawPassword, salt, iterations);
		Base64.Encoder encoder = Base64.getEncoder();
		return String.join("$", PREFIX, String.valueOf(iterations), encoder.encodeToString(salt),
				encoder.encodeToString(key));
	}

	public boolean matches(String rawPassword, String storedHash) {
		String[] parts = storedHash == null ? new String[0] : storedHash.split("\\$");
		if (parts.length != 4 || !PREFIX.equals(parts[0])) {
			return false;
		}
		try {
			Base64.Decoder decoder = Base64.getDecoder();
			byte[] expected = decoder.decode(parts[3]);
			byte[] actual = derive(rawPassword, decoder.decode(parts[2]), Integer.parseInt(parts[1]));
			return MessageDigest.isEqual(expected, actual);
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private static byte[] derive(String password, byte[] salt, int iterations) {
		try {
			PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
			return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Password hashing is unavailable", ex);
		}
	}
}
