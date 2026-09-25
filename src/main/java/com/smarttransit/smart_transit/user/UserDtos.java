package com.smarttransit.smart_transit.user;

import java.time.Instant;

import com.smarttransit.smart_transit.journey.TransportPreference;
import com.smarttransit.smart_transit.network.NetworkDtos.StationDto;
import com.smarttransit.smart_transit.recommendation.RankingStrategy;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public interface UserDtos {

	record RegisterRequest(
			@NotBlank @Email @Size(max = 160) String email,
			@NotBlank @Size(min = 8, max = 72) String password,
			@NotBlank @Size(max = 120) String fullName,
			@Size(max = 20) String phone) {
	}

	record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
	}

	record AuthResponse(String accessToken, Instant expiresAt, UserDto user) {
	}

	record UserDto(Long id, String email, String fullName, String phone, Role role, Long operatorId,
			String operatorName, boolean enabled, Instant createdAt) {

		public static UserDto from(User u) {
			return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(),
					u.getOperator() == null ? null : u.getOperator().getId(),
					u.getOperator() == null ? null : u.getOperator().getName(), u.isEnabled(), u.getCreatedAt());
		}
	}

	record UpdateProfileRequest(@NotBlank @Size(max = 120) String fullName, @Size(max = 20) String phone) {
	}

	record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 72) String newPassword) {
	}

	record AssignRoleRequest(@NotNull Role role, Long operatorId) {
	}

	record SetEnabledRequest(@NotNull Boolean enabled) {
	}

	record PassengerDto(Long id, String fullName, int age, Gender gender, String mobile) {

		public static PassengerDto from(Passenger p) {
			return new PassengerDto(p.getId(), p.getFullName(), p.getAge(), p.getGender(), p.getMobile());
		}
	}

	record PassengerRequest(
			@NotBlank @Size(max = 120) String fullName,
			@Min(1) @Max(120) int age,
			@NotNull Gender gender,
			@Size(max = 20) String mobile) {
	}

	record TravelPreferenceDto(TransportPreference transportPreference, RankingStrategy ranking, int maxTransfers,
			SeatPreference seatPreference, boolean journeyReminders) {

		public static TravelPreferenceDto from(TravelPreference p) {
			return new TravelPreferenceDto(p.getTransportPreference(), p.getRanking(), p.getMaxTransfers(),
					p.getSeatPreference(), p.isJourneyReminders());
		}
	}

	record TravelPreferenceRequest(
			@NotNull TransportPreference transportPreference,
			@NotNull RankingStrategy ranking,
			@Min(0) @Max(2) int maxTransfers,
			@NotNull SeatPreference seatPreference,
			boolean journeyReminders) {
	}

	record SavedJourneyDto(Long id, String label, StationDto origin, StationDto destination,
			TransportPreference transportPreference, RankingStrategy ranking, int passengers) {

		public static SavedJourneyDto from(SavedJourney s) {
			return new SavedJourneyDto(s.getId(), s.getLabel(), StationDto.from(s.getOrigin()),
					StationDto.from(s.getDestination()), s.getTransportPreference(), s.getRanking(), s.getPassengers());
		}
	}

	record SavedJourneyRequest(
			@NotBlank @Size(max = 80) String label,
			@NotNull Long originId,
			@NotNull Long destinationId,
			@NotNull TransportPreference transportPreference,
			@NotNull RankingStrategy ranking,
			@Min(1) @Max(6) int passengers) {
	}
}
