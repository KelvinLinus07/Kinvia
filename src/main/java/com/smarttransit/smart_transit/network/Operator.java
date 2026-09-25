package com.smarttransit.smart_transit.network;

import com.smarttransit.smart_transit.common.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "operators", uniqueConstraints = @UniqueConstraint(name = "uk_operators_code", columnNames = "code"))
public class Operator extends AuditedEntity {

	@Column(nullable = false, length = 16)
	private String code;

	@Column(nullable = false, length = 120)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private TransportMode mode;

	@Column(length = 160)
	private String contactEmail;

	@Column(nullable = false)
	private boolean active = true;

	protected Operator() {
	}

	public Operator(String code, String name, TransportMode mode, String contactEmail) {
		update(code, name, mode, contactEmail, true);
	}

	public void update(String code, String name, TransportMode mode, String contactEmail, boolean active) {
		this.code = code.trim().toUpperCase();
		this.name = name.trim();
		this.mode = mode;
		this.contactEmail = contactEmail;
		this.active = active;
	}

	public String getCode() { return code; }

	public String getName() { return name; }

	public TransportMode getMode() { return mode; }

	public String getContactEmail() { return contactEmail; }

	public boolean isActive() { return active; }
}
