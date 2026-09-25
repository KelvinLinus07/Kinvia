package com.smarttransit.smart_transit.journey;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.smarttransit.smart_transit.common.AuditedEntity;
import com.smarttransit.smart_transit.network.Station;
import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.schedule.Trip;

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

/** One ride within a {@link Journey}: board a trip at one station and alight at another. */
@Entity
@Table(name = "journey_segments",
		uniqueConstraints = @UniqueConstraint(name = "uk_segment_order", columnNames = { "journey_id", "segment_order" }),
		indexes = @Index(name = "idx_segments_trip", columnList = "trip_id"))
public class JourneySegment extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "journey_id", nullable = false)
	private Journey journey;

	@Column(name = "segment_order", nullable = false)
	private int sequence;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	private Trip trip;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "board_station_id", nullable = false)
	private Station boardStation;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "alight_station_id", nullable = false)
	private Station alightStation;

	@Column(nullable = false)
	private int boardSequence;

	@Column(nullable = false)
	private int alightSequence;

	@Column(nullable = false)
	private LocalDateTime departure;

	@Column(nullable = false)
	private LocalDateTime arrival;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private TransportMode mode;

	@Column(nullable = false)
	private int distanceKm;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal fare = BigDecimal.ZERO;

	protected JourneySegment() {
	}

	public JourneySegment(Journey journey, int sequence, Trip trip, Station boardStation, Station alightStation,
			Leg leg) {
		this.journey = journey;
		this.sequence = sequence;
		this.trip = trip;
		this.boardStation = boardStation;
		this.alightStation = alightStation;
		this.boardSequence = leg.boardIndex();
		this.alightSequence = leg.alightIndex();
		this.departure = leg.departure();
		this.arrival = leg.arrival();
		this.mode = leg.trip().mode();
		this.distanceKm = leg.distanceKm();
	}

	public void setFare(BigDecimal fare) { this.fare = fare; }

	public Journey getJourney() { return journey; }

	public int getSequence() { return sequence; }

	public Trip getTrip() { return trip; }

	public Station getBoardStation() { return boardStation; }

	public Station getAlightStation() { return alightStation; }

	public int getBoardSequence() { return boardSequence; }

	public int getAlightSequence() { return alightSequence; }

	public LocalDateTime getDeparture() { return departure; }

	public LocalDateTime getArrival() { return arrival; }

	public TransportMode getMode() { return mode; }

	public int getDistanceKm() { return distanceKm; }

	public BigDecimal getFare() { return fare; }
}
