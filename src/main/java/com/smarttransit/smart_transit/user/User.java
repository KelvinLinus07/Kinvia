package com.smarttransit.smart_transit.user;

import com.smarttransit.smart_transit.common.AuditedEntity;
import com.smarttransit.smart_transit.network.Operator;
import com.smarttransit.smart_transit.security.CurrentUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User extends AuditedEntity {

	@Column(nullable = false, length = 160)
	private String email;

	@Column(nullable = false, length = 200)
	private String passwordHash;

	@Column(nullable = false, length = 120)
	private String fullName;

	@Column(length = 20)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private Role role = Role.PASSENGER;

	/** Set only for {@link Role#OPERATOR} accounts: the operator whose resources this user manages. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "operator_id", foreignKey = @ForeignKey(name = "fk_users_operator"))
	private Operator operator;

	@Column(nullable = false)
	private boolean enabled = true;

	protected User() {
	}

	public User(String email, String passwordHash, String fullName, String phone, Role role, Operator operator) {
		this.email = email.trim().toLowerCase();
		this.passwordHash = passwordHash;
		this.fullName = fullName.trim();
		this.phone = phone;
		this.role = role;
		this.operator = operator;
	}

	public CurrentUser toCurrentUser() {
		return new CurrentUser(getId(), email, fullName, role, operator == null ? null : operator.getId());
	}

	public void updateProfile(String fullName, String phone) {
		this.fullName = fullName.trim();
		this.phone = phone;
	}

	public void changePasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

	public void assignRole(Role role, Operator operator) {
		this.role = role;
		this.operator = role == Role.OPERATOR ? operator : null;
	}

	public void setEnabled(boolean enabled) { this.enabled = enabled; }

	public String getEmail() { return email; }

	public String getPasswordHash() { return passwordHash; }

	public String getFullName() { return fullName; }

	public String getPhone() { return phone; }

	public Role getRole() { return role; }

	public Operator getOperator() { return operator; }

	public boolean isEnabled() { return enabled; }
}
