package com.smarttransit.smart_transit.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.smarttransit.smart_transit.network.CoachClass;

/** The row-to-class mapping and naming rules of an imported service; no database or Spring context needed. */
class TimetableEntryTest {

	@Test
	void trainFaresMapOntoTheClassesTheApplicationHas() {
		TimetableEntry train = TimetableEntry.train("Eastern India Express 0001", null, null, LocalTime.of(1, 17),
				263, 987, 109, 496);

		var fares = train.offeredFares();

		assertEquals(263, fares.get(CoachClass.SLEEPER));
		assertEquals(987, fares.get(CoachClass.AC_1_TIER));
		assertEquals(109, fares.get(CoachClass.AC_2_TIER));
		assertEquals(496, fares.get(CoachClass.AC_3_TIER));
		assertEquals(4, fares.size(), "every train fare column now maps onto a travel class");
		assertEquals(987, train.getFareAc1());
	}

	@Test
	void busSittingFareIsOfferedAndSleeperIsKeptButNotOffered() {
		TimetableEntry bus = TimetableEntry.bus("Eastern Star Travels 0001", null, null, LocalTime.of(0, 23), 199, 348);

		var fares = bus.offeredFares();

		assertEquals(199, fares.get(CoachClass.SEATER));
		assertEquals(1, fares.size());
		assertEquals(348, bus.getFareSleeper());
	}

	@Test
	void serviceNumberComesFromTheEndOfTheName() {
		TimetableEntry train = TimetableEntry.train("Jan Seva Express 80100", null, null, LocalTime.NOON, 1, 1, 1, 1);

		assertEquals(80100, train.getServiceNumber());
		assertEquals("T80100", TimetableCodes.serviceCode(() -> true, train.getServiceNumber()));
	}

	@Test
	void busCodesAreDistinctFromTrainCodes() {
		TimetableEntry bus = TimetableEntry.bus("Royal Roadways 0007", null, null, LocalTime.NOON, 1, 1);

		assertEquals("B00007", TimetableCodes.serviceCode(() -> false, bus.getServiceNumber()));
	}

	@Test
	void aNameWithoutANumberIsRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> TimetableEntry.bus("Nameless Travels", null, null, LocalTime.NOON, 1, 1));
	}

	@Test
	void newEntriesAreNotActivated() {
		assertFalse(TimetableEntry.bus("City Bus 0001", null, null, LocalTime.NOON, 1, 1).isActivated());
	}

	@Test
	void operatorCodesUseTheBrandInitials() {
		assertEquals("EST", TimetableCodes.initials("Eastern Star Travels"));
		assertEquals("NBEB", TimetableCodes.initials("North Bengal Express Bus"));
	}
}
