package com.smarttransit.smart_transit.journey;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.journey.JourneyDtos.JourneyDto;
import com.smarttransit.smart_transit.journey.JourneyDtos.SearchResponse;
import com.smarttransit.smart_transit.recommendation.RankingStrategy;
import com.smarttransit.smart_transit.security.CurrentUser;
import com.smarttransit.smart_transit.security.Public;

/** Journey search and detail. Public so anonymous visitors can browse; results personalise when signed in. */
@RestController
@RequestMapping("/api/journeys")
@Public
public class JourneyController {

	private final JourneySearchService searchService;

	public JourneyController(JourneySearchService searchService) {
		this.searchService = searchService;
	}

	@GetMapping("/search")
	SearchResponse search(CurrentUser user, @RequestParam long originId, @RequestParam long destinationId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(defaultValue = "1") int passengers,
			@RequestParam(required = false) TransportPreference transport,
			@RequestParam(required = false) RankingStrategy ranking,
			@RequestParam(required = false) Integer maxTransfers,
			@RequestParam(required = false) TimeOfDay timeOfDay,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime departAfter,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime departBefore,
			@RequestParam(required = false) BigDecimal maxBudget) {
		JourneyFilter filter = buildFilter(timeOfDay, departAfter, departBefore, maxBudget);
		return searchService.search(
				new SearchQuery(originId, destinationId, date, passengers, transport, ranking, maxTransfers, filter), user);
	}

	/** Either a named band ({@code timeOfDay}) or an explicit range; a half-open range runs to the end/start of the day. */
	private static JourneyFilter buildFilter(TimeOfDay timeOfDay, LocalTime departAfter, LocalTime departBefore,
			BigDecimal maxBudget) {
		boolean customRange = departAfter != null || departBefore != null;
		if (timeOfDay != null && customRange) {
			throw ApiException.badRequest("Choose either timeOfDay or a departAfter/departBefore range, not both");
		}
		if (maxBudget != null && maxBudget.signum() <= 0) {
			throw ApiException.badRequest("Budget must be greater than zero");
		}
		DepartureWindow window = null;
		if (timeOfDay != null) {
			window = timeOfDay.window();
		} else if (customRange) {
			window = new DepartureWindow(departAfter == null ? LocalTime.MIN : departAfter,
					departBefore == null ? LocalTime.of(23, 59) : departBefore);
		}
		return new JourneyFilter(window, maxBudget);
	}

	@GetMapping("/{key}")
	JourneyDto details(@org.springframework.web.bind.annotation.PathVariable String key,
			@RequestParam(defaultValue = "1") int passengers) {
		return searchService.details(key, passengers);
	}
}
