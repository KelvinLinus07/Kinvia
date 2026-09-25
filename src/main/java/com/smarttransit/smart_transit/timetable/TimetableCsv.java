package com.smarttransit.smart_transit.timetable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Reads the bundled timetable CSV files: a header row, then one record per line, values keyed by column name. */
final class TimetableCsv {

	private TimetableCsv() {
	}

	/** Streams the rows of a classpath resource, so a large file never has to be held in memory at once. */
	static void forEachRow(String resource, Consumer<Map<String, String>> handler) {
		try (InputStream in = TimetableCsv.class.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) {
				throw new IllegalStateException("Missing classpath resource " + resource);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String headerLine = reader.readLine();
			if (headerLine == null) {
				throw new IllegalStateException(resource + " is empty");
			}
			List<String> header = splitLine(headerLine);
			String line;
			int lineNumber = 1;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				if (line.isBlank()) {
					continue;
				}
				List<String> values = splitLine(line);
				if (values.size() != header.size()) {
					throw new IllegalStateException(resource + " line " + lineNumber + ": expected " + header.size()
							+ " columns but found " + values.size());
				}
				Map<String, String> row = new HashMap<>();
				for (int i = 0; i < header.size(); i++) {
					row.put(header.get(i), values.get(i));
				}
				handler.accept(row);
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read " + resource, e);
		}
	}

	/** Splits one CSV line; double quotes may wrap a value that contains commas, and "" is an escaped quote. */
	static List<String> splitLine(String line) {
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (quoted) {
				if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					current.append('"');
					i++;
				} else if (c == '"') {
					quoted = false;
				} else {
					current.append(c);
				}
			} else if (c == '"') {
				quoted = true;
			} else if (c == ',') {
				values.add(current.toString());
				current.setLength(0);
			} else {
				current.append(c);
			}
		}
		values.add(current.toString());
		return values;
	}
}
