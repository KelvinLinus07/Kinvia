package com.smarttransit.smart_transit.booking;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smarttransit.smart_transit.security.CurrentUser;
import com.smarttransit.smart_transit.security.Public;

@RestController
@RequestMapping("/api/trips/{tripId}/seats")
public class SeatController {

	private final SeatMapService seatMapService;
	private final SeatHoldService holdService;

	public SeatController(SeatMapService seatMapService, SeatHoldService holdService) {
		this.seatMapService = seatMapService;
		this.holdService = holdService;
	}

	@Public
	@GetMapping
	SeatMapDto seatMap(CurrentUser viewer, @PathVariable long tripId, @RequestParam int board,
			@RequestParam int alight) {
		return seatMapService.seatMap(tripId, board, alight, viewer, false);
	}

	@PostMapping("/{seatId}/hold")
	@ResponseStatus(HttpStatus.CREATED)
	HoldDto hold(CurrentUser user, @PathVariable long tripId, @PathVariable long seatId, @RequestParam int board,
			@RequestParam int alight) {
		return holdService.hold(user, tripId, seatId, board, alight);
	}
}
