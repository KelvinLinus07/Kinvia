package com.smarttransit.smart_transit.security;

import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.user.Role;

/** The authenticated caller, resolved once per request from the access token. */
public record CurrentUser(Long id, String email, String fullName, Role role, Long operatorId) {

	public static final String REQUEST_ATTRIBUTE = CurrentUser.class.getName();

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}

	/** The operator whose data this caller may touch, or null when the caller is an admin (unrestricted). */
	public Long operatorScope() {
		if (isAdmin()) {
			return null;
		}
		if (role != Role.OPERATOR || operatorId == null) {
			throw ApiException.forbidden("This account is not linked to an operator");
		}
		return operatorId;
	}

	public void requireAccessTo(Long resourceOperatorId) {
		Long scope = operatorScope();
		if (scope != null && !scope.equals(resourceOperatorId)) {
			throw ApiException.forbidden("This resource belongs to another operator");
		}
	}
}
