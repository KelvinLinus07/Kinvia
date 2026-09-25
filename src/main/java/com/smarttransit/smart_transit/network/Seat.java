package com.smarttransit.smart_transit.network;

import com.smarttransit.smart_transit.common.AuditedEntity;

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

@Entity
@Table(name = "seats",
		uniqueConstraints = @UniqueConstraint(name = "uk_seats_coach_label", columnNames = { "coach_id", "label" }),
		indexes = @Index(name = "idx_seats_coach", columnList = "coach_id"))
public class Seat extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "coach_id", nullable = false)
	private Coach coach;

	@Column(nullable = false, length = 8)
	private String label;

	@Column(nullable = false)
	private int rowIndex;

	@Column(nullable = false)
	private int columnIndex;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private SeatType seatType;

	protected Seat() {
	}

	Seat(Coach coach, int rowIndex, int columnIndex, SeatType seatType) {
		this.coach = coach;
		this.rowIndex = rowIndex;
		this.columnIndex = columnIndex;
		this.seatType = seatType;
		this.label = (rowIndex + 1) + String.valueOf((char) ('A' + columnIndex));
	}

	public Coach getCoach() { return coach; }

	public String getLabel() { return label; }

	public int getRowIndex() { return rowIndex; }

	public int getColumnIndex() { return columnIndex; }

	public SeatType getSeatType() { return seatType; }
}
