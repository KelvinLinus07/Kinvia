package com.smarttransit.smart_transit.network;

import com.smarttransit.smart_transit.common.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "route_stops",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_route_stops_sequence", columnNames = { "route_id", "stop_sequence" }),
				@UniqueConstraint(name = "uk_route_stops_station", columnNames = { "route_id", "station_id" }) },
		indexes = @Index(name = "idx_route_stops_station", columnList = "station_id"))
public class RouteStop extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "route_id", nullable = false)
	private Route route;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "station_id", nullable = false)
	private Station station;

	@Column(nullable = false, name = "stop_sequence")
	private int sequence;

	/** Minutes after the origin departure at which the vehicle arrives here. */
	@Column(nullable = false)
	private int arrivalOffsetMinutes;

	@Column(nullable = false)
	private int departureOffsetMinutes;

	@Column(nullable = false)
	private int distanceKm;

	protected RouteStop() {
	}

	RouteStop(Route route, Station station, int sequence, int arrivalOffsetMinutes, int departureOffsetMinutes,
			int distanceKm) {
		this.route = route;
		this.station = station;
		this.sequence = sequence;
		this.arrivalOffsetMinutes = arrivalOffsetMinutes;
		this.departureOffsetMinutes = departureOffsetMinutes;
		this.distanceKm = distanceKm;
	}

	public Route getRoute() { return route; }

	public Station getStation() { return station; }

	public int getSequence() { return sequence; }

	public int getArrivalOffsetMinutes() { return arrivalOffsetMinutes; }

	public int getDepartureOffsetMinutes() { return departureOffsetMinutes; }

	public int getDistanceKm() { return distanceKm; }
}
