package com.smarttransit.smart_transit.journey;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.smarttransit.smart_transit.common.ApiException;

/**
 * URL-safe, stateless identifier of a planned journey, e.g. {@code 12.0.4~57.1.3}. Because journeys are recomputed
 * from live data on demand, a key can be shared, bookmarked or reloaded without server-side session state.
 */
public final class JourneyKey {

	private static final int MAX_SEGMENTS = 4;

	private JourneyKey() {
	}

	public static String format(List<SegmentRef> segments) {
		return segments.stream().map(s -> s.tripId() + "." + s.boardSequence() + "." + s.alightSequence())
				.collect(Collectors.joining("~"));
	}

	public static List<SegmentRef> parse(String key) {
		if (key == null || key.isBlank()) {
			throw ApiException.badRequest("Journey key is required");
		}
		String[] parts = key.split("~");
		if (parts.length > MAX_SEGMENTS) {
			throw ApiException.badRequest("Journey key is invalid");
		}
		List<SegmentRef> segments = new ArrayList<>();
		try {
			for (String part : parts) {
				String[] fields = part.split("\\.");
				if (fields.length != 3) {
					throw new NumberFormatException();
				}
				SegmentRef ref = new SegmentRef(Long.parseLong(fields[0]), Integer.parseInt(fields[1]),
						Integer.parseInt(fields[2]));
				if (ref.boardSequence() < 0 || ref.alightSequence() <= ref.boardSequence()) {
					throw new NumberFormatException();
				}
				segments.add(ref);
			}
		}
		catch (NumberFormatException ex) {
			throw ApiException.badRequest("Journey key is invalid");
		}
		return segments;
	}
}
