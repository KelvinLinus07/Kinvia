package com.smarttransit.smart_transit.journey;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.smarttransit.smart_transit.network.CoachClass;
import com.smarttransit.smart_transit.network.NetworkDtos.StationDto;
import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.recommendation.JourneyTag;
import com.smarttransit.smart_transit.recommendation.RankingStrategy;
import com.smarttransit.smart_transit.schedule.TripStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public interface JourneyDtos {

	/**
	 * {@code matchedBeforeFilters} is how many journeys the route, date and party size produced before the time and
	 * budget limits in {@code filter} were applied, so the UI can tell "nothing runs" from "nothing fits your limits".
	 */
	record SearchResponse(StationDto origin, StationDto destination, LocalDate date, int passengers,
			TransportPreference transport, RankingStrategy ranking, JourneyFilter filter, int matchedBeforeFilters,
			List<JourneyDto> journeys) {
	}

	record JourneyDto(String key, LocalDateTime departure, LocalDateTime arrival, long durationMinutes,
			long waitMinutes, int transfers, boolean multimodal, List<TransportMode> modes,
			BigDecimal farePerPassenger, BigDecimal totalFare, int availableSeats, boolean bookable, double score,
			List<JourneyTag> tags, List<String> highlights, List<SegmentDto> segments) {
	}

	record SegmentDto(int index, long tripId, TransportMode mode, String routeCode, String routeName,
			String operatorName, String vehicleNumber, String vehicleName, List<String> amenities,
			StopDto board, StopDto alight, int boardSequence, int alightSequence, LocalDateTime departure,
			LocalDateTime arrival, long durationMinutes, int distanceKm, TripStatus status, int delayMinutes,
			String statusNote, Long waitBeforeMinutes, List<StopDto> stops, List<ClassOfferDto> classes) {
	}

	record StopDto(Long stationId, String code, String name, String city, double latitude, double longitude,
			int sequence, LocalDateTime arrival, LocalDateTime departure) {

		static StopDto from(StopTime s) {
			return new StopDto(s.stationId(), s.stationCode(), s.stationName(), s.city(), s.latitude(),
					s.longitude(), s.sequence(), s.arrival(), s.departure());
		}
	}

	record ClassOfferDto(CoachClass coachClass, String label, BigDecimal fare, int availableSeats) {

		static ClassOfferDto from(ClassOffer o) {
			return new ClassOfferDto(o.coachClass(), o.coachClass().label(), o.fare(), o.availableSeats());
		}
	}

	record InterpretRequest(@NotBlank @Size(max = 300) String text) {
	}

	/** What the smart-search parser understood; unrecognised parts are listed so the UI can ask about them. */
	record InterpretResponse(Long originId, String originName, Long destinationId, String destinationName,
			LocalDate date, Integer passengers, TransportPreference transport, RankingStrategy ranking,
			List<String> notes) {
	}
}
