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

/**
 * An ordered list of stops with time and distance offsets from the origin. Stop sequences are always contiguous
 * (0..n-1) so a sequence doubles as an index.
 */
@Entity
@Table(name = "routes",
		uniqueConstraints = @UniqueConstraint(name = "uk_routes_code", columnNames = "code"),
		indexes = @Index(name = "idx_routes_operator", columnList = "operator_id"))
public class Route extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "operator_id", nullable = false)
	private Operator operator;

	@Column(nullable = false, length = 24)
	private String code;

	@Column(nullable = false, length = 160)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private TransportMode mode;

	@Column(nullable = false)
	private boolean active = true;

	@OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sequence")
	private List<RouteStop> stops = new ArrayList<>();

	@OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("coachClass")
	private List<RouteFare> fares = new ArrayList<>();

	protected Route() {
	}

	public Route(Operator operator, String code, String name) {
		this.operator = operator;
		this.mode = operator.getMode();
		this.code = code.trim().toUpperCase();
		this.name = name.trim();
	}

	public void rename(String code, String name, boolean active) {
		this.code = code.trim().toUpperCase();
		this.name = name.trim();
		this.active = active;
	}

	public RouteStop addStop(Station station, int arrivalOffsetMinutes, int departureOffsetMinutes, int distanceKm) {
		RouteStop stop = new RouteStop(this, station, stops.size(), arrivalOffsetMinutes, departureOffsetMinutes,
				distanceKm);
		stops.add(stop);
		return stop;
	}

	public void clearStops() {
		stops.clear();
	}

	public RouteFare setFare(CoachClass coachClass, java.math.BigDecimal ratePerKm, java.math.BigDecimal minimumFare) {
		for (RouteFare fare : fares) {
			if (fare.getCoachClass() == coachClass) {
				fare.update(ratePerKm, minimumFare);
				return fare;
			}
		}
		RouteFare fare = new RouteFare(this, coachClass, ratePerKm, minimumFare);
		fares.add(fare);
		return fare;
	}

	public void retainFares(java.util.Set<CoachClass> classes) {
		fares.removeIf(f -> !classes.contains(f.getCoachClass()));
	}

	public Operator getOperator() { return operator; }

	public String getCode() { return code; }

	public String getName() { return name; }

	public TransportMode getMode() { return mode; }

	public boolean isActive() { return active; }

	public List<RouteStop> getStops() { return stops; }

	public List<RouteFare> getFares() { return fares; }
}
