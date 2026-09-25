package com.smarttransit.smart_transit.user;

import com.smarttransit.smart_transit.common.AuditedEntity;
import com.smarttransit.smart_transit.journey.TransportPreference;
import com.smarttransit.smart_transit.network.Station;
import com.smarttransit.smart_transit.recommendation.RankingStrategy;

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

/** A favourite search (origin, destination and preferences) a traveller can re-run with one tap. */
@Entity
@Table(name = "saved_journeys",
		uniqueConstraints = @UniqueConstraint(name = "uk_saved_journey", columnNames = { "user_id", "origin_id", "destination_id" }),
		indexes = @Index(name = "idx_saved_journeys_user", columnList = "user_id"))
public class SavedJourney extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 80)
	private String label;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "origin_id", nullable = false)
	private Station origin;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "destination_id", nullable = false)
	private Station destination;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private TransportPreference transportPreference;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RankingStrategy ranking;

	@Column(nullable = false)
	private int passengers;

	protected SavedJourney() {
	}

	public SavedJourney(User user, String label, Station origin, Station destination,
			TransportPreference transportPreference, RankingStrategy ranking, int passengers) {
		this.user = user;
		this.label = label.trim();
		this.origin = origin;
		this.destination = destination;
		this.transportPreference = transportPreference;
		this.ranking = ranking;
		this.passengers = passengers;
	}

	public User getUser() { return user; }

	public String getLabel() { return label; }

	public Station getOrigin() { return origin; }

	public Station getDestination() { return destination; }

	public TransportPreference getTransportPreference() { return transportPreference; }

	public RankingStrategy getRanking() { return ranking; }

	public int getPassengers() { return passengers; }
}
