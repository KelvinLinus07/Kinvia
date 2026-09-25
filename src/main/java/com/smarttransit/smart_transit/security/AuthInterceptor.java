package com.smarttransit.smart_transit.security;

import java.util.Arrays;
import java.util.OptionalLong;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.user.User;
import com.smarttransit.smart_transit.user.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Default-deny authorization for the REST API: an endpoint needs a valid token unless it is annotated
 * {@link Public}, and {@link RequiresRole} narrows access further. A valid token is always resolved when present, so
 * public endpoints can still personalise their response.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

	private static final String BEARER = "Bearer ";

	private final AccessTokenService tokens;
	private final UserRepository users;

	public AuthInterceptor(AccessTokenService tokens, UserRepository users) {
		this.tokens = tokens;
		this.users = users;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod method)) {
			return true;
		}
		CurrentUser user = authenticate(request);
		request.setAttribute(CurrentUser.REQUEST_ATTRIBUTE, user);

		if (annotation(method, Public.class) != null) {
			return true;
		}
		if (user == null) {
			throw ApiException.unauthorized("Sign in to continue");
		}
		RequiresRole required = annotation(method, RequiresRole.class);
		if (required != null && !Arrays.asList(required.value()).contains(user.role())) {
			throw ApiException.forbidden("Your account does not have access to this resource");
		}
		return true;
	}

	private CurrentUser authenticate(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith(BEARER)) {
			return null;
		}
		OptionalLong userId = tokens.verify(header.substring(BEARER.length()).trim());
		if (userId.isEmpty()) {
			return null;
		}
		return users.findWithOperatorById(userId.getAsLong()).filter(User::isEnabled).map(User::toCurrentUser)
				.orElse(null);
	}

	private static <A extends java.lang.annotation.Annotation> A annotation(HandlerMethod method, Class<A> type) {
		A onMethod = method.getMethodAnnotation(type);
		return onMethod != null ? onMethod : AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), type);
	}
}
