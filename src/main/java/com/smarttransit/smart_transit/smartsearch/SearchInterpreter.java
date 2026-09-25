package com.smarttransit.smart_transit.smartsearch;

import com.smarttransit.smart_transit.journey.JourneyDtos.InterpretRequest;
import com.smarttransit.smart_transit.journey.JourneyDtos.InterpretResponse;

/**
 * Turns a free-text request ("cheapest bus from Siliguri to Patna next Friday for 2 people") into a structured
 * search. {@link RuleBasedSearchInterpreter} needs no external service and works fully offline; an
 * {@code kinvia.ai.*}-configured LLM-backed implementation can be registered as the primary bean later without
 * changing the controller or the rest of search.
 */
public interface SearchInterpreter {

	InterpretResponse interpret(InterpretRequest request);
}
