package com.smarttransit.smart_transit.smartsearch;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smarttransit.smart_transit.journey.JourneyDtos.InterpretRequest;
import com.smarttransit.smart_transit.journey.JourneyDtos.InterpretResponse;
import com.smarttransit.smart_transit.security.Public;

import jakarta.validation.Valid;

/** Natural-language search entry point. Works without any AI provider configured; see {@link SearchInterpreter}. */
@RestController
@RequestMapping("/api/journeys/interpret")
@Public
public class SmartSearchController {

	private final SearchInterpreter interpreter;

	public SmartSearchController(SearchInterpreter interpreter) {
		this.interpreter = interpreter;
	}

	@PostMapping
	InterpretResponse interpret(@Valid @RequestBody InterpretRequest request) {
		return interpreter.interpret(request);
	}
}
