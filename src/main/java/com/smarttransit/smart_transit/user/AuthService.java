package com.smarttransit.smart_transit.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.security.AccessTokenService;
import com.smarttransit.smart_transit.security.PasswordHasher;
import com.smarttransit.smart_transit.user.UserDtos.AuthResponse;
import com.smarttransit.smart_transit.user.UserDtos.LoginRequest;
import com.smarttransit.smart_transit.user.UserDtos.RegisterRequest;
import com.smarttransit.smart_transit.user.UserDtos.UserDto;

@Service
public class AuthService {

	private final UserRepository users;
	private final TravelPreferenceRepository preferences;
	private final PasswordHasher passwordHasher;
	private final AccessTokenService tokens;
	private final java.time.Clock clock;

	public AuthService(UserRepository users, TravelPreferenceRepository preferences, PasswordHasher passwordHasher,
			AccessTokenService tokens, java.time.Clock clock) {
		this.users = users;
		this.preferences = preferences;
		this.passwordHasher = passwordHasher;
		this.tokens = tokens;
		this.clock = clock;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (users.existsByEmailIgnoreCase(request.email())) {
			throw ApiException.conflict("An account with this email already exists");
		}
		User user = users.save(new User(request.email(), passwordHasher.hash(request.password()), request.fullName(),
				request.phone(), Role.PASSENGER, null));
		preferences.save(new TravelPreference(user));
		return issueSession(user);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		User user = users.findByEmailIgnoreCase(request.email())
				.filter(u -> passwordHasher.matches(request.password(), u.getPasswordHash()))
				.orElseThrow(() -> ApiException.unauthorized("Incorrect email or password"));
		if (!user.isEnabled()) {
			throw ApiException.forbidden("This account has been disabled");
		}
		return issueSession(user);
	}

	private AuthResponse issueSession(User user) {
		String token = tokens.issue(user.getId());
		return new AuthResponse(token, java.time.Instant.now(clock).plus(tokens.ttl()), UserDto.from(user));
	}
}
