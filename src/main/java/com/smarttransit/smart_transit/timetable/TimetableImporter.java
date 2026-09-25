package com.smarttransit.smart_transit.timetable;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.smarttransit.smart_transit.config.KinviaProperties;
import com.smarttransit.smart_transit.network.Station;
import com.smarttransit.smart_transit.network.StationKind;
import com.smarttransit.smart_transit.network.StationRepository;

/**
 * Loads the transport workbook's stations and services (bundled as CSV under {@code resources/data}, see
 * {@code tools/build_timetable_csv.py}) into the database on start-up. It only runs while the timetable table is
 * empty, never touches other data, and can be switched off with {@code KINVIA_TIMETABLE_IMPORT=false}.
 * <p>
 * Stations that already exist by name (for example from the development seed) are reused, not duplicated.
 */
@Component
@Order(200)
public class TimetableImporter implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(TimetableImporter.class);

	private static final String STATIONS = "data/stations.csv";
	private static final String TRAINS = "data/train_services.csv";
	private static final String BUSES = "data/bus_services.csv";
	private static final int CHUNK_SIZE = 2000;
	private static final int MAX_CODE_LENGTH = 12;
	private static final int TRANSFER_MINUTES = 30;

	private final KinviaProperties properties;
	private final StationRepository stations;
	private final TimetableEntryRepository entries;
	private final TransactionTemplate transaction;

	public TimetableImporter(KinviaProperties properties, StationRepository stations,
			TimetableEntryRepository entries, PlatformTransactionManager transactionManager) {
		this.properties = properties;
		this.stations = stations;
		this.entries = entries;
		this.transaction = new TransactionTemplate(transactionManager);
	}

	@Override
	public void run(String... args) {
		if (!properties.timetable().importEnabled()) {
			return;
		}
		if (entries.count() > 0) {
			log.info("[TIMETABLE] Timetable already imported; skipping.");
			return;
		}
		log.info("[TIMETABLE] Importing stations and services from the bundled workbook data...");
		try {
			Map<String, Long> stationIds = transaction.execute(status -> importStations());
			int trains = importServices(TRAINS, stationIds, true);
			int buses = importServices(BUSES, stationIds, false);
			log.info("[TIMETABLE] Imported {} train and {} bus services.", trains, buses);
		} catch (RuntimeException e) {
			// Chunks are committed as they go; drop them so the next start retries from a clean timetable.
			log.error("[TIMETABLE] Import failed; removing the partially imported services.", e);
			entries.deleteAllInBatch();
			throw e;
		}
	}

	private Map<String, Long> importStations() {
		Map<String, Long> ids = new HashMap<>();
		int created = 0;
		int reused = 0;
		List<Map<String, String>> rows = new ArrayList<>();
		TimetableCsv.forEachRow(STATIONS, rows::add);
		for (Map<String, String> row : rows) {
			String name = row.get("name");
			Station station = stations.findFirstByNameIgnoreCase(name).orElse(null);
			if (station == null) {
				station = stations.save(new Station(uniqueCode(row.get("code")), name, row.get("city"),
						row.get("state"), StationKind.HUB, Double.parseDouble(row.get("latitude")),
						Double.parseDouble(row.get("longitude")), TRANSFER_MINUTES));
				created++;
			} else {
				reused++;
			}
			ids.put(name, station.getId());
		}
		log.info("[TIMETABLE] Stations: {} created, {} already present.", created, reused);
		return ids;
	}

	/** The workbook has no codes, so the bundled ones are internal; add a digit if another station already has it. */
	private String uniqueCode(String wanted) {
		String code = wanted.trim().toUpperCase();
		int suffix = 1;
		while (stations.existsByCodeIgnoreCase(code)) {
			String tail = String.valueOf(++suffix);
			code = wanted.trim().toUpperCase();
			code = code.substring(0, Math.min(code.length(), MAX_CODE_LENGTH - tail.length())) + tail;
		}
		return code;
	}

	private int importServices(String resource, Map<String, Long> stationIds, boolean trains) {
		List<Map<String, String>> chunk = new ArrayList<>(CHUNK_SIZE);
		int[] saved = { 0 };
		int[] lowSecondAc = { 0 };
		TimetableCsv.forEachRow(resource, row -> {
			chunk.add(row);
			if (chunk.size() == CHUNK_SIZE) {
				saved[0] += saveChunk(chunk, stationIds, trains, lowSecondAc);
				chunk.clear();
			}
		});
		if (!chunk.isEmpty()) {
			saved[0] += saveChunk(chunk, stationIds, trains, lowSecondAc);
		}
		if (lowSecondAc[0] > 0) {
			log.warn("[TIMETABLE] {} train services list a 2nd AC fare below their Sleeper fare. Imported exactly as "
					+ "supplied; check the '2nd AC' column of the workbook.", lowSecondAc[0]);
		}
		return saved[0];
	}

	private int saveChunk(List<Map<String, String>> rows, Map<String, Long> stationIds, boolean trains,
			int[] lowSecondAc) {
		return transaction.execute(status -> {
			List<TimetableEntry> batch = new ArrayList<>(rows.size());
			for (Map<String, String> row : rows) {
				Station origin = stations.getReferenceById(stationId(stationIds, row.get("from")));
				Station destination = stations.getReferenceById(stationId(stationIds, row.get("to")));
				LocalTime departure = LocalTime.parse(row.get("departure"));
				if (trains) {
					int sleeper = Integer.parseInt(row.get("fare_sl"));
					int secondAc = Integer.parseInt(row.get("fare_2ac"));
					if (secondAc < sleeper) {
						lowSecondAc[0]++;
					}
					batch.add(TimetableEntry.train(row.get("name"), origin, destination, departure, sleeper,
							Integer.parseInt(row.get("fare_1ac")), secondAc, Integer.parseInt(row.get("fare_3ac"))));
				} else {
					batch.add(TimetableEntry.bus(row.get("name"), origin, destination, departure,
							Integer.parseInt(row.get("fare_sitting")), Integer.parseInt(row.get("fare_sleeper"))));
				}
			}
			entries.saveAll(batch);
			return batch.size();
		});
	}

	private static long stationId(Map<String, Long> ids, String name) {
		Long id = ids.get(name);
		if (id == null) {
			throw new IllegalStateException("Timetable refers to a station that is not in stations.csv: " + name);
		}
		return id;
	}
}
