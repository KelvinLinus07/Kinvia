package com.smarttransit.smart_transit.user;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.common.PageResponse;
import com.smarttransit.smart_transit.network.Operator;
import com.smarttransit.smart_transit.network.OperatorRepository;
import com.smarttransit.smart_transit.security.PasswordHasher;
import com.smarttransit.smart_transit.user.UserDtos.AssignRoleRequest;
import com.smarttransit.smart_transit.user.UserDtos.ChangePasswordRequest;
import com.smarttransit.smart_transit.user.UserDtos.PassengerDto;
import com.smarttransit.smart_transit.user.UserDtos.PassengerRequest;
import com.smarttransit.smart_transit.user.UserDtos.SavedJourneyDto;
import com.smarttransit.smart_transit.user.UserDtos.SavedJourneyRequest;
import com.smarttransit.smart_transit.user.UserDtos.TravelPreferenceDto;
import com.smarttransit.smart_transit.user.UserDtos.TravelPreferenceRequest;
import com.smarttransit.smart_transit.user.UserDtos.UpdateProfileRequest;
import com.smarttransit.smart_transit.user.UserDtos.UserDto;
import com.smarttransit.smart_transit.network.Station;
import com.smarttransit.smart_transit.network.StationRepository;

@Service
public class UserService {

	private final UserRepository users;
	private final PassengerRepository passengers;
	private final TravelPreferenceRepository preferences;
	private final SavedJourneyRepository savedJourneys;
	private final StationRepository stations;
	private final OperatorRepository operators;
	private final PasswordHasher passwordHasher;

	public UserService(UserRepository users, PassengerRepository passengers, TravelPreferenceRepository preferences,
			SavedJourneyRepository savedJourneys, StationRepository stations, OperatorRepository operators,
			PasswordHasher passwordHasher) {
		this.users = users;
		this.passengers = passengers;
		this.preferences = preferences;
		this.savedJourneys = savedJourneys;
		this.stations = stations;
		this.operators = operators;
		this.passwordHasher = passwordHasher;
	}

	@Transactional(readOnly = true)
	public UserDto profile(Long userId) {
		return UserDto.from(user(userId));
	}

	@Transactional
	public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
		User user = user(userId);
		user.updateProfile(request.fullName(), request.phone());
		return UserDto.from(user);
	}

	@Transactional
	public void changePassword(Long userId, ChangePasswordRequest request) {
		User user = user(userId);
		if (!passwordHasher.matches(request.currentPassword(), user.getPasswordHash())) {
			throw ApiException.badRequest("Current password is incorrect");
		}
		user.changePasswordHash(passwordHasher.hash(request.newPassword()));
	}

	@Transactional(readOnly = true)
	public List<PassengerDto> passengers(Long userId) {
		return passengers.findByUserIdOrderByFullName(userId).stream().map(PassengerDto::from).toList();
	}

	@Transactional
	public PassengerDto addPassenger(Long userId, PassengerRequest request) {
		Passenger passenger = new Passenger(users.getReferenceById(userId), request.fullName(), request.age(),
				request.gender(), request.mobile());
		return PassengerDto.from(passengers.save(passenger));
	}

	@Transactional
	public PassengerDto updatePassenger(Long userId, Long id, PassengerRequest request) {
		Passenger passenger = passengers.findByIdAndUserId(id, userId)
				.orElseThrow(() -> ApiException.notFound("Passenger", id));
		passenger.update(request.fullName(), request.age(), request.gender(), request.mobile());
		return PassengerDto.from(passenger);
	}

	@Transactional
	public void deletePassenger(Long userId, Long id) {
		Passenger passenger = passengers.findByIdAndUserId(id, userId)
				.orElseThrow(() -> ApiException.notFound("Passenger", id));
		passengers.delete(passenger);
	}

	@Transactional(readOnly = true)
	public TravelPreferenceDto preferences(Long userId) {
		return TravelPreferenceDto.from(preferenceFor(userId));
	}

	@Transactional
	public TravelPreferenceDto updatePreferences(Long userId, TravelPreferenceRequest request) {
		TravelPreference preference = preferenceFor(userId);
		preference.update(request.transportPreference(), request.ranking(), request.maxTransfers(),
				request.seatPreference(), request.journeyReminders());
		return TravelPreferenceDto.from(preference);
	}

	private TravelPreference preferenceFor(Long userId) {
		return preferences.findByUserId(userId).orElseGet(() -> preferences.save(new TravelPreference(user(userId))));
	}

	@Transactional(readOnly = true)
	public List<SavedJourneyDto> savedJourneys(Long userId) {
		return savedJourneys.findByUserIdOrderByCreatedAtDesc(userId).stream().map(SavedJourneyDto::from).toList();
	}

	@Transactional
	public SavedJourneyDto saveJourney(Long userId, SavedJourneyRequest request) {
		if (request.originId().equals(request.destinationId())) {
			throw ApiException.badRequest("Origin and destination must be different");
		}
		if (savedJourneys.existsByUserIdAndOriginIdAndDestinationId(userId, request.originId(), request.destinationId())) {
			throw ApiException.conflict("You already saved this journey");
		}
		Station origin = stations.findById(request.originId()).orElseThrow(() -> ApiException.notFound("Station", request.originId()));
		Station destination = stations.findById(request.destinationId()).orElseThrow(() -> ApiException.notFound("Station", request.destinationId()));
		SavedJourney saved = new SavedJourney(users.getReferenceById(userId), request.label(), origin, destination,
				request.transportPreference(), request.ranking(), request.passengers());
		return SavedJourneyDto.from(savedJourneys.save(saved));
	}

	@Transactional
	public void deleteSavedJourney(Long userId, Long id) {
		SavedJourney saved = savedJourneys.findByIdAndUserId(id, userId)
				.orElseThrow(() -> ApiException.notFound("Saved journey", id));
		savedJourneys.delete(saved);
	}

	@Transactional(readOnly = true)
	public PageResponse<UserDto> search(String query, int page, int size) {
		return PageResponse.of(users.search(query == null ? "" : query.toLowerCase(), PageRequest.of(page, size)),
				UserDto::from);
	}

	@Transactional
	public UserDto assignRole(Long id, AssignRoleRequest request) {
		User user = user(id);
		Operator operator = null;
		if (request.role() == Role.OPERATOR) {
			operator = operators.findById(request.operatorId())
					.orElseThrow(() -> ApiException.badRequest("Choose an operator for this account"));
		}
		user.assignRole(request.role(), operator);
		return UserDto.from(user);
	}

	@Transactional
	public UserDto setEnabled(Long id, boolean enabled) {
		User user = user(id);
		user.setEnabled(enabled);
		return UserDto.from(user);
	}

	private User user(Long id) {
		return users.findById(id).orElseThrow(() -> ApiException.notFound("User", id));
	}
}
