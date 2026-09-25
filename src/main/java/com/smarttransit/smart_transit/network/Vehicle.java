package com.smarttransit.smart_transit.network;

import java.util.ArrayList;
import java.util.List;

import com.smarttransit.smart_transit.common.AuditedEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** A train or a bus. Coaches are append-only once created because seat reservations refer to them. */
@Entity
@Table(name = "vehicles",
		uniqueConstraints = @UniqueConstraint(name = "uk_vehicles_number", columnNames = "number"),
		indexes = @Index(name = "idx_vehicles_operator", columnList = "operator_id"))
public class Vehicle extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "operator_id", nullable = false)
	private Operator operator;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private TransportMode mode;

	/** Train number or bus registration. */
	@Column(nullable = false, length = 24)
	private String number;

	@Column(nullable = false, length = 120)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private VehicleStatus status = VehicleStatus.ACTIVE;

	/** Comma separated amenity labels, e.g. "Charging points,Blankets". */
	@Column(length = 240)
	private String amenities = "";

	@OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("code")
	private List<Coach> coaches = new ArrayList<>();

	protected Vehicle() {
	}

	public Vehicle(Operator operator, String number, String name, String amenities) {
		this.operator = operator;
		this.mode = operator.getMode();
		this.number = number.trim().toUpperCase();
		this.name = name.trim();
		this.amenities = amenities == null ? "" : amenities;
	}

	public void update(String name, VehicleStatus status, String amenities) {
		this.name = name.trim();
		this.status = status;
		this.amenities = amenities == null ? "" : amenities;
	}

	public Coach addCoach(String code, CoachClass coachClass, int rows, int columns, int aisleAfter) {
		Coach coach = new Coach(this, code, coachClass, rows, columns, aisleAfter);
		coaches.add(coach);
		return coach;
	}

	public Operator getOperator() { return operator; }

	public TransportMode getMode() { return mode; }

	public String getNumber() { return number; }

	public String getName() { return name; }

	public VehicleStatus getStatus() { return status; }

	public String getAmenities() { return amenities; }

	public List<Coach> getCoaches() { return coaches; }
}
