package com.smarttransit.smart_transit.user;

import com.smarttransit.smart_transit.common.AuditedEntity;
import com.smarttransit.smart_transit.journey.TransportPreference;
import com.smarttransit.smart_transit.recommendation.RankingStrategy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Defaults that personalise search and ranking when the traveller does not choose explicitly. */
@Entity
@Table(name = "travel_preferences", uniqueConstraints = @UniqueConstraint(name = "uk_pref_user", columnNames = "user_id"))
public class TravelPreference extends AuditedEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private TransportPreference transportPreference = TransportPreference.ANY;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RankingStrategy ranking = RankingStrategy.BALANCED;

	@Column(nullable = false)
	private int maxTransfers = 2;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private SeatPreference seatPreference = SeatPreference.NO_PREFERENCE;

	@Column(nullable = false)
	private boolean journeyReminders = true;

	protected TravelPreference() {
	}

	public TravelPreference(User user) {
		this.user = user;
	}

	public void update(TransportPreference transportPreference, RankingStrategy ranking, int maxTransfers,
			SeatPreference seatPreference, boolean journeyReminders) {
		this.transportPreference = transportPreference;
		this.ranking = ranking;
		this.maxTransfers = maxTransfers;
		this.seatPreference = seatPreference;
		this.journeyReminders = journeyReminders;
	}

	public TransportPreference getTransportPreference() { return transportPreference; }

	public RankingStrategy getRanking() { return ranking; }

	public int getMaxTransfers() { return maxTransfers; }

	public SeatPreference getSeatPreference() { return seatPreference; }

	public boolean isJourneyReminders() { return journeyReminders; }
}
