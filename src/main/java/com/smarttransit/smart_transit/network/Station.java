package com.smarttransit.smart_transit.network;

import com.smarttransit.smart_transit.common.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** A place where vehicles stop. Transfers between services happen only at the same station. */
@Entity
@Table(name = "stations",
		uniqueConstraints = @UniqueConstraint(name = "uk_stations_code", columnNames = "code"),
		indexes = @Index(name = "idx_stations_name", columnList = "name"))
public class Station extends AuditedEntity {

	@Column(nullable = false, length = 12)
	private String code;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, length = 80)
	private String city;

	@Column(nullable = false, length = 80)
	private String state;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private StationKind kind;

	@Column(nullable = false)
	private double latitude;

	@Column(nullable = false)
	private double longitude;

	/** Minimum time a traveller needs to change vehicles here. */
	@Column(nullable = false)
	private int transferMinutes = 30;

	@Column(nullable = false)
	private boolean active = true;

	protected Station() {
	}

	public Station(String code, String name, String city, String state, StationKind kind, double latitude,
			double longitude, int transferMinutes) {
		update(code, name, city, state, kind, latitude, longitude, transferMinutes, true);
	}

	public void update(String code, String name, String city, String state, StationKind kind, double latitude,
			double longitude, int transferMinutes, boolean active) {
		this.code = code.trim().toUpperCase();
		this.name = name.trim();
		this.city = city.trim();
		this.state = state.trim();
		this.kind = kind;
		this.latitude = latitude;
		this.longitude = longitude;
		this.transferMinutes = transferMinutes;
		this.active = active;
	}

	public String getCode() { return code; }

	public String getName() { return name; }

	public String getCity() { return city; }

	public String getState() { return state; }

	public StationKind getKind() { return kind; }

	public double getLatitude() { return latitude; }

	public double getLongitude() { return longitude; }

	public int getTransferMinutes() { return transferMinutes; }

	public boolean isActive() { return active; }
}
