package com.smarttransit.smart_transit.journey;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.smarttransit.smart_transit.journey.JourneyDtos.ClassOfferDto;
import com.smarttransit.smart_transit.journey.JourneyDtos.JourneyDto;
import com.smarttransit.smart_transit.journey.JourneyDtos.SegmentDto;
import com.smarttransit.smart_transit.journey.JourneyDtos.StopDto;
import com.smarttransit.smart_transit.recommendation.JourneyTag;
import com.smarttransit.smart_transit.recommendation.ScoredItinerary;

@Component
public class JourneyMapper {

	public JourneyDto toDto(ScoredItinerary scored, int passengers, int optionCount) {
		Itinerary itinerary = scored.itinerary();
		BigDecimal perPassenger = itinerary.farePerPassenger(passengers);
		List<SegmentDto> segments = new ArrayList<>();
		for (int i = 0; i < itinerary.legs().size(); i++) {
			segments.add(toSegment(itinerary, i));
		}
		return new JourneyDto(itinerary.key(), itinerary.departure(), itinerary.arrival(),
				itinerary.duration().toMinutes(), itinerary.waiting().toMinutes(), itinerary.transfers(),
				itinerary.isMultimodal(), itinerary.legs().stream().map(l -> l.trip().mode()).toList(), perPassenger,
				perPassenger.multiply(BigDecimal.valueOf(passengers)), itinerary.availableSeats(),
				itinerary.isBookableFor(passengers), scored.score(), scored.tags().stream().sorted().toList(),
				JourneyExplainer.highlights(scored, passengers, optionCount), segments);
	}

	private SegmentDto toSegment(Itinerary itinerary, int index) {
		Leg leg = itinerary.legs().get(index);
		TripSnapshot trip = leg.trip();
		Long wait = index == 0 ? null : itinerary.waitBefore(index).toMinutes();
		return new SegmentDto(index, trip.tripId(), trip.mode(), trip.routeCode(), trip.routeName(),
				trip.operatorName(), trip.vehicleNumber(), trip.vehicleName(), trip.amenities(),
				StopDto.from(leg.board()), StopDto.from(leg.alight()), leg.boardIndex(), leg.alightIndex(),
				leg.departure(), leg.arrival(), leg.duration().toMinutes(), leg.distanceKm(), trip.status(),
				trip.delayMinutes(), trip.statusNote(), wait, trip.stops().stream().map(StopDto::from).toList(),
				leg.offers().stream().map(ClassOfferDto::from).toList());
	}

	static String modeLabel(com.smarttransit.smart_transit.network.TransportMode mode) {
		return mode.name().toLowerCase(Locale.ROOT);
	}

	static boolean has(ScoredItinerary scored, JourneyTag tag) {
		return scored.tags().contains(tag);
	}
}
