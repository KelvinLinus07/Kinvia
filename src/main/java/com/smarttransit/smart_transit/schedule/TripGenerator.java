package com.smarttransit.smart_transit.schedule;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.config.KinviaProperties;

/** Materialises dated trips from recurring schedules over a rolling window. Idempotent. */
@Service
public class TripGenerator {

	private static final Logger log = LoggerFactory.getLogger(TripGenerator.class);

	private final ScheduleRepository schedules;
	private final TripRepository trips;
	private final KinviaProperties properties;
	private final Clock clock;

	public TripGenerator(ScheduleRepository schedules, TripRepository trips, KinviaProperties properties, Clock clock) {
		this.schedules = schedules;
		this.trips = trips;
		this.properties = properties;
		this.clock = clock;
	}

	/** Covers the configured history and horizon around today. */
	@Scheduled(cron = "0 15 2 * * *", zone = "${kinvia.zone:Asia/Kolkata}")
	public void generateRollingWindow() {
		LocalDate today = LocalDate.now(clock);
		int created = generate(today.minusDays(properties.trips().historyDays()),
				today.plusDays(properties.trips().horizonDays()));
		log.info("Trip generation created {} trips", created);
	}

	@Transactional
	public int generate(LocalDate from, LocalDate to) {
		List<Schedule> active = schedules.findByActiveTrue();
		int created = 0;
		for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
			Set<Long> existing = new HashSet<>(trips.findScheduledIdsOn(date));
			for (Schedule schedule : active) {
				if (schedule.runsOn(date) && !existing.contains(schedule.getId())) {
					trips.save(new Trip(schedule, date));
					created++;
				}
			}
		}
		return created;
	}
}
