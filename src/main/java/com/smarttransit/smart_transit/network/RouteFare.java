package com.smarttransit.smart_transit.network;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.smarttransit.smart_transit.common.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Distance-based pricing for one travel class on one route. */
@Entity
@Table(name = "route_fares",
		uniqueConstraints = @UniqueConstraint(name = "uk_route_fares_class", columnNames = { "route_id", "coach_class" }))
public class RouteFare extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "route_id", nullable = false)
	private Route route;

	@Enumerated(EnumType.STRING)
	@Column(name = "coach_class", nullable = false, length = 16)
	private CoachClass coachClass;

	@Column(nullable = false, precision = 8, scale = 3)
	private BigDecimal ratePerKm;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal minimumFare;

	protected RouteFare() {
	}

	RouteFare(Route route, CoachClass coachClass, BigDecimal ratePerKm, BigDecimal minimumFare) {
		this.route = route;
		this.coachClass = coachClass;
		update(ratePerKm, minimumFare);
	}

	void update(BigDecimal ratePerKm, BigDecimal minimumFare) {
		this.ratePerKm = ratePerKm;
		this.minimumFare = minimumFare;
	}

	/** Whole-rupee fare for a distance, never below the class minimum. */
	public BigDecimal fareForDistance(int distanceKm) {
		BigDecimal byDistance = ratePerKm.multiply(BigDecimal.valueOf(distanceKm)).setScale(0, RoundingMode.HALF_UP);
		return byDistance.max(minimumFare).setScale(2, RoundingMode.HALF_UP);
	}

	public Route getRoute() { return route; }

	public CoachClass getCoachClass() { return coachClass; }

	public BigDecimal getRatePerKm() { return ratePerKm; }

	public BigDecimal getMinimumFare() { return minimumFare; }
}
