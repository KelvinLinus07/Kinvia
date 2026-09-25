package com.smarttransit.smart_transit.network;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.smarttransit.smart_transit.schedule.Schedule;
import com.smarttransit.smart_transit.schedule.Trip;
import com.smarttransit.smart_transit.schedule.TripStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request and response shapes for the transport network: stations, operators, fleet, routes, schedules, trips. */
public interface NetworkDtos {

	record StationDto(Long id, String code, String name, String city, String state, StationKind kind,
			double latitude, double longitude, int transferMinutes, boolean active) {

		public static StationDto from(Station s) {
			return new StationDto(s.getId(), s.getCode(), s.getName(), s.getCity(), s.getState(), s.getKind(),
					s.getLatitude(), s.getLongitude(), s.getTransferMinutes(), s.isActive());
		}
	}

	record StationRequest(
			@NotBlank @Size(max = 12) String code,
			@NotBlank @Size(max = 120) String name,
			@NotBlank @Size(max = 80) String city,
			@NotBlank @Size(max = 80) String state,
			@NotNull StationKind kind,
			@DecimalMin("-90") @DecimalMax("90") double latitude,
			@DecimalMin("-180") @DecimalMax("180") double longitude,
			@Min(5) @Max(240) int transferMinutes,
			Boolean active) {
	}

	record OperatorDto(Long id, String code, String name, TransportMode mode, String contactEmail, boolean active) {

		public static OperatorDto from(Operator o) {
			return new OperatorDto(o.getId(), o.getCode(), o.getName(), o.getMode(), o.getContactEmail(), o.isActive());
		}
	}

	record OperatorRequest(
			@NotBlank @Size(max = 16) String code,
			@NotBlank @Size(max = 120) String name,
			@NotNull TransportMode mode,
			@Size(max = 160) String contactEmail,
			Boolean active) {
	}

	record CoachDto(Long id, String code, CoachClass coachClass, String classLabel, int rows, int columns,
			int aisleAfter, int capacity) {

		public static CoachDto from(Coach c) {
			return new CoachDto(c.getId(), c.getCode(), c.getCoachClass(), c.getCoachClass().label(),
					c.getSeatRows(), c.getSeatColumns(), c.getAisleAfter(), c.getCapacity());
		}
	}

	record VehicleDto(Long id, Long operatorId, String operatorName, TransportMode mode, String number, String name,
			VehicleStatus status, List<String> amenities, List<CoachDto> coaches, int totalSeats) {

		public static VehicleDto from(Vehicle v) {
			List<CoachDto> coaches = v.getCoaches().stream().map(CoachDto::from).toList();
			List<String> amenities = v.getAmenities().isBlank() ? List.of()
					: Arrays.stream(v.getAmenities().split(",")).map(String::trim).toList();
			return new VehicleDto(v.getId(), v.getOperator().getId(), v.getOperator().getName(), v.getMode(),
					v.getNumber(), v.getName(), v.getStatus(), amenities, coaches,
					coaches.stream().mapToInt(CoachDto::capacity).sum());
		}
	}

	record CoachRequest(
			@NotBlank @Size(max = 12) String code,
			@NotNull CoachClass coachClass,
			@Min(1) @Max(40) int rows,
			@Min(1) @Max(8) int columns,
			@Min(0) @Max(7) int aisleAfter) {
	}

	record VehicleRequest(
			Long operatorId,
			@NotBlank @Size(max = 24) String number,
			@NotBlank @Size(max = 120) String name,
			@Size(max = 240) String amenities,
			VehicleStatus status,
			@Valid List<CoachRequest> coaches) {
	}

	record RouteStopDto(int sequence, Long stationId, String stationCode, String stationName, int arrivalOffsetMinutes,
			int departureOffsetMinutes, int distanceKm) {

		public static RouteStopDto from(RouteStop s) {
			return new RouteStopDto(s.getSequence(), s.getStation().getId(), s.getStation().getCode(),
					s.getStation().getName(), s.getArrivalOffsetMinutes(), s.getDepartureOffsetMinutes(),
					s.getDistanceKm());
		}
	}

	record FareDto(CoachClass coachClass, String classLabel, BigDecimal ratePerKm, BigDecimal minimumFare) {

		public static FareDto from(RouteFare f) {
			return new FareDto(f.getCoachClass(), f.getCoachClass().label(), f.getRatePerKm(), f.getMinimumFare());
		}
	}

	record RouteDto(Long id, Long operatorId, String operatorName, String code, String name, TransportMode mode,
			boolean active, List<RouteStopDto> stops, List<FareDto> fares) {

		public static RouteDto from(Route r) {
			return new RouteDto(r.getId(), r.getOperator().getId(), r.getOperator().getName(), r.getCode(),
					r.getName(), r.getMode(), r.isActive(), r.getStops().stream().map(RouteStopDto::from).toList(),
					r.getFares().stream().map(FareDto::from).toList());
		}
	}

	record RouteStopRequest(
			@NotNull Long stationId,
			@Min(0) int arrivalOffsetMinutes,
			@Min(0) int departureOffsetMinutes,
			@Min(0) int distanceKm) {
	}

	record FareRequest(
			@NotNull CoachClass coachClass,
			@NotNull @DecimalMin("0.01") BigDecimal ratePerKm,
			@NotNull @DecimalMin("0") BigDecimal minimumFare) {
	}

	record RouteRequest(
			Long operatorId,
			@NotBlank @Size(max = 24) String code,
			@NotBlank @Size(max = 160) String name,
			Boolean active,
			@NotEmpty @Size(min = 2, max = 40) @Valid List<RouteStopRequest> stops,
			@NotEmpty @Valid List<FareRequest> fares) {
	}

	record ScheduleDto(Long id, Long routeId, String routeName, Long vehicleId, String vehicleName,
			LocalTime departureTime, Set<DayOfWeek> days, boolean active) {

		public static ScheduleDto from(Schedule s) {
			return new ScheduleDto(s.getId(), s.getRoute().getId(), s.getRoute().getName(), s.getVehicle().getId(),
					s.getVehicle().getName(), s.getDepartureTime(), s.getDays(), s.isActive());
		}
	}

	record ScheduleRequest(
			@NotNull Long routeId,
			@NotNull Long vehicleId,
			@NotNull LocalTime departureTime,
			@NotEmpty Set<DayOfWeek> days,
			Boolean active) {
	}

	record TripDto(Long id, Long routeId, String routeName, String routeCode, TransportMode mode, Long vehicleId,
			String vehicleName, String vehicleNumber, String operatorName, LocalDate serviceDate,
			LocalDateTime departure, TripStatus status, int delayMinutes, String statusNote) {

		public static TripDto from(Trip t) {
			return new TripDto(t.getId(), t.getRoute().getId(), t.getRoute().getName(), t.getRoute().getCode(),
					t.getRoute().getMode(), t.getVehicle().getId(), t.getVehicle().getName(),
					t.getVehicle().getNumber(), t.getRoute().getOperator().getName(), t.getServiceDate(),
					t.getDeparture(), t.getStatus(), t.getDelayMinutes(), t.getStatusNote());
		}
	}

	record TripStatusRequest(
			@NotNull TripStatus status,
			@Min(0) @Max(1440) int delayMinutes,
			@Size(max = 240) String note) {
	}
}
