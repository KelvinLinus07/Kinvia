package com.smarttransit.smart_transit.booking;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smarttransit.smart_transit.security.CurrentUser;

@RestController
@RequestMapping("/api/holds")
public class HoldController {

	private final SeatHoldService holdService;

	public HoldController(SeatHoldService holdService) {
		this.holdService = holdService;
	}

	@GetMapping
	List<HoldDto> mine(CurrentUser user) {
		return holdService.myHolds(user);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void release(CurrentUser user, @PathVariable long id) {
		holdService.release(user, id);
	}
}
