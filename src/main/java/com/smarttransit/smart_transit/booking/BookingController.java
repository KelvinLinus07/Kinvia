package com.smarttransit.smart_transit.booking;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smarttransit.smart_transit.booking.BookingDtos.BookingDto;
import com.smarttransit.smart_transit.booking.BookingDtos.BookingSummaryDto;
import com.smarttransit.smart_transit.booking.BookingDtos.CancellationPreview;
import com.smarttransit.smart_transit.booking.BookingDtos.CreateBookingRequest;
import com.smarttransit.smart_transit.common.PageResponse;
import com.smarttransit.smart_transit.security.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

	private final BookingService bookings;

	public BookingController(BookingService bookings) {
		this.bookings = bookings;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	BookingDto create(CurrentUser user, @Valid @RequestBody CreateBookingRequest request) {
		return bookings.create(user, request);
	}

	@GetMapping
	PageResponse<BookingSummaryDto> list(CurrentUser user, @RequestParam(defaultValue = "ALL") BookingScope scope,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		return bookings.list(user, scope, page, size);
	}

	@GetMapping("/{id}")
	BookingDto get(CurrentUser user, @PathVariable Long id) {
		return bookings.get(user, id);
	}

	@GetMapping("/{id}/cancellation-preview")
	CancellationPreview preview(CurrentUser user, @PathVariable Long id) {
		return bookings.previewCancellation(user, id);
	}

	@PostMapping("/{id}/cancel")
	BookingDto cancel(CurrentUser user, @PathVariable Long id) {
		return bookings.cancel(user, id);
	}
}
