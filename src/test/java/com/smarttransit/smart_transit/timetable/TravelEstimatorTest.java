package com.smarttransit.smart_transit.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.smarttransit.smart_transit.network.TransportMode;

class TravelEstimatorTest {

	// Howrah Junction and Patna Junction, roughly 470 km apart in a straight line.
	private static final double HOWRAH_LAT = 22.5839, HOWRAH_LNG = 88.3425, PATNA_LAT = 25.6093, PATNA_LNG = 85.1376;

	@Test
	void greatCircleDistanceIsPlausible() {
		double km = TravelEstimator.greatCircleKm(HOWRAH_LAT, HOWRAH_LNG, PATNA_LAT, PATNA_LNG);

		assertTrue(km > 450 && km < 490, "Howrah to Patna is about 470 km as the crow flies, got " + km);
	}

	@Test
	void trainsAreFasterThanBusesOnTheSameRoute() {
		var train = TravelEstimator.estimate(TransportMode.TRAIN, HOWRAH_LAT, HOWRAH_LNG, PATNA_LAT, PATNA_LNG);
		var bus = TravelEstimator.estimate(TransportMode.BUS, HOWRAH_LAT, HOWRAH_LNG, PATNA_LAT, PATNA_LNG);

		assertTrue(train.minutes() < bus.minutes());
		assertEquals(train.distanceKm(), bus.distanceKm());
	}

	@Test
	void veryShortHopsStillTakeAFewMinutes() {
		var estimate = TravelEstimator.estimate(TransportMode.TRAIN, 22.5839, 88.3425, 22.5697, 88.3697);

		assertTrue(estimate.minutes() >= 15);
		assertTrue(estimate.distanceKm() >= 1);
	}

	@Test
	void minutesAreRoundedToFive() {
		var estimate = TravelEstimator.estimate(TransportMode.BUS, HOWRAH_LAT, HOWRAH_LNG, PATNA_LAT, PATNA_LNG);

		assertEquals(0, estimate.minutes() % 5);
	}
}
