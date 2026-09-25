package com.smarttransit.smart_transit.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Checks the CSV reader and the bundled data files themselves, without a database. */
class TimetableCsvTest {

	@Test
	void splitsPlainAndQuotedValues() {
		assertEquals(List.of("a", "b c", "d"), TimetableCsv.splitLine("a,b c,d"));
		assertEquals(List.of("Motihari, Bihar", "x"), TimetableCsv.splitLine("\"Motihari, Bihar\",x"));
		assertEquals(List.of("say \"hi\"", "y"), TimetableCsv.splitLine("\"say \"\"hi\"\"\",y"));
		assertEquals(List.of("", "b", ""), TimetableCsv.splitLine(",b,"));
	}

	@Test
	void everyServiceRefersToAKnownStationAndHasAUniqueNumber() {
		Set<String> stations = new HashSet<>();
		Set<String> codes = new HashSet<>();
		TimetableCsv.forEachRow("data/stations.csv", row -> {
			assertTrue(stations.add(row.get("name")), "duplicate station " + row.get("name"));
			assertTrue(codes.add(row.get("code")), "duplicate code " + row.get("code"));
			assertTrue(row.get("code").length() <= 12);
		});
		assertEquals(90, stations.size());

		for (String file : List.of("data/train_services.csv", "data/bus_services.csv")) {
			Set<String> names = new HashSet<>();
			int[] rows = { 0 };
			TimetableCsv.forEachRow(file, row -> {
				rows[0]++;
				assertTrue(stations.contains(row.get("from")), "unknown station " + row.get("from"));
				assertTrue(stations.contains(row.get("to")), "unknown station " + row.get("to"));
				assertTrue(!row.get("from").equals(row.get("to")));
				assertTrue(names.add(row.get("name")), "duplicate service " + row.get("name"));
			});
			assertEquals(rows[0], names.size());
		}
	}

	@Test
	void everyDirectedStationPairHasTenTrainsAndFiveBuses() {
		Map<String, Integer> trains = count("data/train_services.csv");
		Map<String, Integer> buses = count("data/bus_services.csv");

		assertEquals(90 * 89, trains.size());
		assertEquals(90 * 89, buses.size());
		assertTrue(trains.values().stream().allMatch(n -> n == 10));
		assertTrue(buses.values().stream().allMatch(n -> n == 5));
	}

	private static Map<String, Integer> count(String file) {
		Map<String, Integer> counts = new java.util.HashMap<>();
		List<String> keys = new ArrayList<>();
		TimetableCsv.forEachRow(file, row -> keys.add(row.get("from") + "->" + row.get("to")));
		keys.forEach(k -> counts.merge(k, 1, Integer::sum));
		return counts;
	}
}
