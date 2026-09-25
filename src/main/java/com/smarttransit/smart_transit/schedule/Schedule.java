package com.smarttransit.smart_transit.schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

import com.smarttransit.smart_transit.common.AuditedEntity;
import com.smarttransit.smart_transit.network.Route;
import com.smarttransit.smart_transit.network.Vehicle;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A recurring service: a vehicle runs a route at a fixed origin departure time on given weekdays. */
@Entity
@Table(name = "schedules", indexes = @Index(name = "idx_schedules_route", columnList = "route_id"))
public class Schedule extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "route_id", nullable = false)
	private Route route;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vehicle_id", nullable = false)
	private Vehicle vehicle;

	@Column(nullable = false)
	private LocalTime departureTime;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "schedule_days", joinColumns = @JoinColumn(name = "schedule_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "day_of_week", length = 12)
	private Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);

	@Column(nullable = false)
	private boolean active = true;

	protected Schedule() {
	}

	public Schedule(Route route, Vehicle vehicle, LocalTime departureTime, Set<DayOfWeek> days) {
		update(route, vehicle, departureTime, days, true);
	}

	public void update(Route route, Vehicle vehicle, LocalTime departureTime, Set<DayOfWeek> days, boolean active) {
		this.route = route;
		this.vehicle = vehicle;
		this.departureTime = departureTime;
		this.days = days.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(days);
		this.active = active;
	}

	public boolean runsOn(java.time.LocalDate date) {
		return active && days.contains(date.getDayOfWeek());
	}

	public Route getRoute() { return route; }

	public Vehicle getVehicle() { return vehicle; }

	public LocalTime getDepartureTime() { return departureTime; }

	public Set<DayOfWeek> getDays() { return days; }

	public boolean isActive() { return active; }
}
