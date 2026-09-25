package com.smarttransit.smart_transit.user;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smarttransit.smart_transit.security.CurrentUser;
import com.smarttransit.smart_transit.user.UserDtos.ChangePasswordRequest;
import com.smarttransit.smart_transit.user.UserDtos.PassengerDto;
import com.smarttransit.smart_transit.user.UserDtos.PassengerRequest;
import com.smarttransit.smart_transit.user.UserDtos.SavedJourneyDto;
import com.smarttransit.smart_transit.user.UserDtos.SavedJourneyRequest;
import com.smarttransit.smart_transit.user.UserDtos.TravelPreferenceDto;
import com.smarttransit.smart_transit.user.UserDtos.TravelPreferenceRequest;
import com.smarttransit.smart_transit.user.UserDtos.UpdateProfileRequest;
import com.smarttransit.smart_transit.user.UserDtos.UserDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	UserDto profile(CurrentUser user) {
		return userService.profile(user.id());
	}

	@PutMapping
	UserDto updateProfile(CurrentUser user, @Valid @RequestBody UpdateProfileRequest request) {
		return userService.updateProfile(user.id(), request);
	}

	@PostMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void changePassword(CurrentUser user, @Valid @RequestBody ChangePasswordRequest request) {
		userService.changePassword(user.id(), request);
	}

	@GetMapping("/passengers")
	List<PassengerDto> passengers(CurrentUser user) {
		return userService.passengers(user.id());
	}

	@PostMapping("/passengers")
	@ResponseStatus(HttpStatus.CREATED)
	PassengerDto addPassenger(CurrentUser user, @Valid @RequestBody PassengerRequest request) {
		return userService.addPassenger(user.id(), request);
	}

	@PutMapping("/passengers/{id}")
	PassengerDto updatePassenger(CurrentUser user, @PathVariable Long id, @Valid @RequestBody PassengerRequest request) {
		return userService.updatePassenger(user.id(), id, request);
	}

	@DeleteMapping("/passengers/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deletePassenger(CurrentUser user, @PathVariable Long id) {
		userService.deletePassenger(user.id(), id);
	}

	@GetMapping("/preferences")
	TravelPreferenceDto preferences(CurrentUser user) {
		return userService.preferences(user.id());
	}

	@PutMapping("/preferences")
	TravelPreferenceDto updatePreferences(CurrentUser user, @Valid @RequestBody TravelPreferenceRequest request) {
		return userService.updatePreferences(user.id(), request);
	}

	@GetMapping("/saved-journeys")
	List<SavedJourneyDto> savedJourneys(CurrentUser user) {
		return userService.savedJourneys(user.id());
	}

	@PostMapping("/saved-journeys")
	@ResponseStatus(HttpStatus.CREATED)
	SavedJourneyDto saveJourney(CurrentUser user, @Valid @RequestBody SavedJourneyRequest request) {
		return userService.saveJourney(user.id(), request);
	}

	@DeleteMapping("/saved-journeys/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteSavedJourney(CurrentUser user, @PathVariable Long id) {
		userService.deleteSavedJourney(user.id(), id);
	}
}
