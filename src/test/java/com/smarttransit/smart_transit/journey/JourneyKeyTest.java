package com.smarttransit.smart_transit.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.smarttransit.smart_transit.common.ApiException;

class JourneyKeyTest {

	@Test
	void formatAndParseRoundTripForASingleSegment() {
		List<SegmentRef> segments = List.of(new SegmentRef(42, 0, 3));
		String key = JourneyKey.format(segments);
		assertEquals(segments, JourneyKey.parse(key));
	}

	@Test
	void formatAndParseRoundTripForMultipleSegments() {
		List<SegmentRef> segments = List.of(new SegmentRef(1, 0, 2), new SegmentRef(2, 0, 5), new SegmentRef(3, 1, 4));
		assertEquals(segments, JourneyKey.parse(JourneyKey.format(segments)));
	}

	@Test
	void rejectsBlankKeys() {
		assertThrows(ApiException.class, () -> JourneyKey.parse(""));
		assertThrows(ApiException.class, () -> JourneyKey.parse(null));
	}

	@Test
	void rejectsMalformedSegments() {
		assertThrows(ApiException.class, () -> JourneyKey.parse("notanumber.0.1"));
		assertThrows(ApiException.class, () -> JourneyKey.parse("1.0")); // missing a field
	}

	@Test
	void rejectsASegmentWhereAlightIsNotAfterBoard() {
		assertThrows(ApiException.class, () -> JourneyKey.parse("1.3.3"));
		assertThrows(ApiException.class, () -> JourneyKey.parse("1.3.1"));
	}

	@Test
	void rejectsTooManySegments() {
		assertThrows(ApiException.class, () -> JourneyKey.parse("1.0.1~2.0.1~3.0.1~4.0.1~5.0.1"));
	}
}
