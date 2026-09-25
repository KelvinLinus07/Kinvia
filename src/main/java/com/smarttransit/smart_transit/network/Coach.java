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

/** A rectangular seat grid. {@code aisleAfter} is the number of columns before the aisle (0 = no aisle). */
@Entity
@Table(name = "coaches",
		uniqueConstraints = @UniqueConstraint(name = "uk_coaches_vehicle_code", columnNames = { "vehicle_id", "code" }),
		indexes = @Index(name = "idx_coaches_vehicle", columnList = "vehicle_id"))
public class Coach extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vehicle_id", nullable = false)
	private Vehicle vehicle;

	@Column(nullable = false, length = 12)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private CoachClass coachClass;

	@Column(nullable = false)
	private int seatRows;

	@Column(nullable = false)
	private int seatColumns;

	@Column(nullable = false)
	private int aisleAfter;

	@Column(nullable = false)
	private int capacity;

	@OneToMany(mappedBy = "coach", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("rowIndex, columnIndex")
	private List<Seat> seats = new ArrayList<>();

	protected Coach() {
	}

	Coach(Vehicle vehicle, String code, CoachClass coachClass, int rows, int columns, int aisleAfter) {
		this.vehicle = vehicle;
		this.code = code.trim().toUpperCase();
		this.coachClass = coachClass;
		this.seatRows = rows;
		this.seatColumns = columns;
		this.aisleAfter = aisleAfter;
		this.capacity = rows * columns;
		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < columns; column++) {
				seats.add(new Seat(this, row, column, seatTypeAt(column)));
			}
		}
	}

	private SeatType seatTypeAt(int column) {
		if (column == 0 || column == seatColumns - 1) {
			return SeatType.WINDOW;
		}
		boolean besideAisle = aisleAfter > 0 && (column == aisleAfter - 1 || column == aisleAfter);
		return besideAisle ? SeatType.AISLE : SeatType.MIDDLE;
	}

	public Vehicle getVehicle() { return vehicle; }

	public String getCode() { return code; }

	public CoachClass getCoachClass() { return coachClass; }

	public int getSeatRows() { return seatRows; }

	public int getSeatColumns() { return seatColumns; }

	public int getAisleAfter() { return aisleAfter; }

	public int getCapacity() { return capacity; }

	public List<Seat> getSeats() { return seats; }
}
