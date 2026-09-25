package com.smarttransit.smart_transit.journey;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.smarttransit.smart_transit.common.AuditedEntity;
import com.smarttransit.smart_transit.network.Station;
import com.smarttransit.smart_transit.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * A traveller's itinerary as it was booked: an ordered list of {@link JourneySegment}s, each on its own trip. A
 * journey exists independently of any single vehicle, so one booking can span buses and trains.
 */
@Entity
@Table(name = "journeys", indexes = @Index(name = "idx_journeys_user", columnList = "user_id"))
public class Journey extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "origin_id", nullable = false)
	private Station origin;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "destination_id", nullable = false)
	private Station destination;

	@Column(nullable = false)
	private LocalDateTime departure;

	@Column(nullable = false)
	private LocalDateTime arrival;

	@Column(nullable = false)
	private int transfers;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal totalFare = BigDecimal.ZERO;

	@OneToMany(mappedBy = "journey", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sequence")
	private List<JourneySegment> segments = new ArrayList<>();

	protected Journey() {
	}

	public Journey(User user, Station origin, Station destination, LocalDateTime departure, LocalDateTime arrival) {
		this.user = user;
		this.origin = origin;
		this.destination = destination;
		this.departure = departure;
		this.arrival = arrival;
	}

	public JourneySegment addSegment(JourneySegment segment) {
		segments.add(segment);
		transfers = segments.size() - 1;
		return segment;
	}

	public void setTotalFare(BigDecimal totalFare) { this.totalFare = totalFare; }

	public User getUser() { return user; }

	public Station getOrigin() { return origin; }

	public Station getDestination() { return destination; }

	public LocalDateTime getDeparture() { return departure; }

	public LocalDateTime getArrival() { return arrival; }

	public int getTransfers() { return transfers; }

	public BigDecimal getTotalFare() { return totalFare; }

	public List<JourneySegment> getSegments() { return segments; }
}
