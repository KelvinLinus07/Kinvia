package com.smarttransit.smart_transit.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.smarttransit.smart_transit.common.AuditedEntity;
import com.smarttransit.smart_transit.network.Route;
import com.smarttransit.smart_transit.network.Vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One dated run of a schedule. Seat reservations are made against a trip. */
@Entity
@Table(name = "trips",
		uniqueConstraints = @UniqueConstraint(name = "uk_trips_schedule_date", columnNames = { "schedule_id", "service_date" }),
		indexes = { @Index(name = "idx_trips_date_status", columnList = "service_date, status"),
				@Index(name = "idx_trips_route", columnList = "route_id") })
public class Trip extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "schedule_id", nullable = false)
	private Schedule schedule;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "route_id", nullable = false)
	private Route route;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vehicle_id", nullable = false)
	private Vehicle vehicle;

	@Column(name = "service_date", nullable = false)
	private LocalDate serviceDate;

	/** Scheduled departure from the route origin. */
	@Column(nullable = false)
	private LocalDateTime departure;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private TripStatus status = TripStatus.SCHEDULED;

	@Column(nullable = false)
	private int delayMinutes;

	@Column(length = 240)
	private String statusNote;

	protected Trip() {
	}

	public Trip(Schedule schedule, LocalDate serviceDate) {
		this.schedule = schedule;
		this.route = schedule.getRoute();
		this.vehicle = schedule.getVehicle();
		this.serviceDate = serviceDate;
		this.departure = serviceDate.atTime(schedule.getDepartureTime());
	}

	public void updateStatus(TripStatus status, int delayMinutes, String note) {
		this.status = status;
		this.delayMinutes = status == TripStatus.CANCELLED ? 0 : delayMinutes;
		this.statusNote = note;
	}

	public Schedule getSchedule() { return schedule; }

	public Route getRoute() { return route; }

	public Vehicle getVehicle() { return vehicle; }

	public LocalDate getServiceDate() { return serviceDate; }

	public LocalDateTime getDeparture() { return departure; }

	public TripStatus getStatus() { return status; }

	public int getDelayMinutes() { return delayMinutes; }

	public String getStatusNote() { return statusNote; }
}
