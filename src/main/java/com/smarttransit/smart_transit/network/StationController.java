package com.smarttransit.smart_transit.network;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smarttransit.smart_transit.network.NetworkDtos.StationDto;
import com.smarttransit.smart_transit.security.Public;

/** Read-only station lookup used by search boxes across the app; no authentication required. */
@RestController
@RequestMapping("/api/stations")
@Public
public class StationController {

	private final StationRepository stations;

	public StationController(StationRepository stations) {
		this.stations = stations;
	}

	/** Type-ahead lookup: partial name, city or station code. Without a query, the first stations by name. */
	@GetMapping
	List<StationDto> list(@RequestParam(required = false) String q, @RequestParam(defaultValue = "20") int limit) {
		int size = Math.min(Math.max(limit, 1), 50);
		List<Station> result = (q == null || q.isBlank())
				? stations.findByActiveTrueOrderByName().stream().limit(size).toList()
				: stations.search(q.trim().toLowerCase(Locale.ROOT), PageRequest.of(0, size));
		return result.stream().map(StationDto::from).toList();
	}
}
