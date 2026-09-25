package com.smarttransit.smart_transit.timetable;

import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.smarttransit.smart_transit.common.AuditedEntity;
import com.smarttransit.smart_transit.network.CoachClass;
import com.smarttransit.smart_transit.network.Route;
import com.smarttransit.smart_transit.network.Station;
import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.schedule.Schedule;

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

/**
 * One row of the imported transport workbook: a daily point-to-point train or bus service with its departure time
 * and the fare for each class it lists. The row is kept exactly as supplied (all fare columns included). The first
 * time someone searches the pair, {@link TimetableActivator} turns it into a real route, vehicle and schedule and
 * records them here.
 */
@Entity
@Table(name = "timetable_entries",
		uniqueConstraints = @UniqueConstraint(name = "uk_timetable_mode_number", columnNames = { "mode", "service_number" }),
		indexes = @Index(name = "idx_timetable_pair", columnList = "origin_id, destination_id"))
public class TimetableEntry extends AuditedEntity {

	private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)\\s*$");

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private TransportMode mode;

	/** The number at the end of the service name, unique within a mode. */
	@Column(name = "service_number", nullable = false)
	private int serviceNumber;

	@Column(nullable = false, length = 120)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "origin_id", nullable = false)
	private Station origin;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "destination_id", nullable = false)
	private Station destination;

	@Column(nullable = false)
	private LocalTime departureTime;

	// Train fares (Sleeper, 1st AC, 2nd AC, 3rd AC) - null on bus rows.
	private Integer fareSl;
	private Integer fareAc1;
	private Integer fareAc2;
	private Integer fareAc3;

	// Bus fares (Sitting, Sleeper) - null on train rows.
	private Integer fareSitting;
	private Integer fareSleeper;

	/** Set once the service has been activated. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "route_id")
	private Route route;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "schedule_id")
	private Schedule schedule;

	protected TimetableEntry() {
	}

	private TimetableEntry(TransportMode mode, String name, Station origin, Station destination, LocalTime departureTime) {
		Matcher number = TRAILING_NUMBER.matcher(name);
		if (!number.find()) {
			throw new IllegalArgumentException("Service name has no trailing number: " + name);
		}
		this.mode = mode;
		this.serviceNumber = Integer.parseInt(number.group(1));
		this.name = name.trim();
		this.origin = origin;
		this.destination = destination;
		this.departureTime = departureTime;
	}

	public static TimetableEntry train(String name, Station origin, Station destination, LocalTime departureTime,
			int fareSl, int fareAc1, int fareAc2, int fareAc3) {
		TimetableEntry entry = new TimetableEntry(TransportMode.TRAIN, name, origin, destination, departureTime);
		entry.fareSl = fareSl;
		entry.fareAc1 = fareAc1;
		entry.fareAc2 = fareAc2;
		entry.fareAc3 = fareAc3;
		return entry;
	}

	public static TimetableEntry bus(String name, Station origin, Station destination, LocalTime departureTime,
			int fareSitting, int fareSleeper) {
		TimetableEntry entry = new TimetableEntry(TransportMode.BUS, name, origin, destination, departureTime);
		entry.fareSitting = fareSitting;
		entry.fareSleeper = fareSleeper;
		return entry;
	}

	/**
	 * The fares that map onto a travel class the application already has. Every train fare column now has a
	 * matching {@link CoachClass}; bus Sleeper still has none, so it stays in the stored row but is not offered
	 * for booking.
	 */
	public Map<CoachClass, Integer> offeredFares() {
		Map<CoachClass, Integer> fares = new EnumMap<>(CoachClass.class);
		if (mode == TransportMode.TRAIN) {
			fares.put(CoachClass.SLEEPER, fareSl);
			fares.put(CoachClass.AC_3_TIER, fareAc3);
			fares.put(CoachClass.AC_2_TIER, fareAc2);
			fares.put(CoachClass.AC_1_TIER, fareAc1);
		} else {
			fares.put(CoachClass.SEATER, fareSitting);
		}
		return fares;
	}

	public boolean isActivated() {
		return schedule != null;
	}

	public void markActivated(Route route, Schedule schedule) {
		this.route = route;
		this.schedule = schedule;
	}

	public TransportMode getMode() { return mode; }

	public int getServiceNumber() { return serviceNumber; }

	public String getName() { return name; }

	public Station getOrigin() { return origin; }

	public Station getDestination() { return destination; }

	public LocalTime getDepartureTime() { return departureTime; }

	public Integer getFareSl() { return fareSl; }

	public Integer getFareAc1() { return fareAc1; }

	public Integer getFareAc2() { return fareAc2; }

	public Integer getFareAc3() { return fareAc3; }

	public Integer getFareSitting() { return fareSitting; }

	public Integer getFareSleeper() { return fareSleeper; }

	public Route getRoute() { return route; }

	public Schedule getSchedule() { return schedule; }
}
