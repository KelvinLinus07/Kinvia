package com.smarttransit.smart_transit.smartsearch;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.smarttransit.smart_transit.journey.JourneyDtos.InterpretRequest;
import com.smarttransit.smart_transit.journey.JourneyDtos.InterpretResponse;
import com.smarttransit.smart_transit.journey.TransportPreference;
import com.smarttransit.smart_transit.network.Station;
import com.smarttransit.smart_transit.network.StationRepository;
import com.smarttransit.smart_transit.recommendation.RankingStrategy;

/**
 * Pattern and keyword based interpreter: no model, no external call, fully deterministic. It recognises
 * "from X to Y", weekday and relative-date words ("today", "tomorrow", "next friday"), a passenger count, a
 * transport word (train/bus/any) and a ranking word (cheapest/fastest/comfortable/direct). Anything it cannot map
 * confidently is left null and explained in {@code notes} so the UI can ask the traveller directly.
 */
@Component
public class RuleBasedSearchInterpreter implements SearchInterpreter {

	private static final Pattern ROUTE = Pattern.compile(
			"from\\s+([a-zA-Z .]+?)\\s+to\\s+([a-zA-Z .]+?)(?=\\s+(?:on|next|tomorrow|today|for|this)\\b|$)",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PASSENGERS = Pattern.compile("for\\s+(\\d+)\\s*(?:people|passengers|persons|adults)?",
			Pattern.CASE_INSENSITIVE);

	private final StationRepository stations;

	public RuleBasedSearchInterpreter(StationRepository stations) {
		this.stations = stations;
	}

	@Override
	public InterpretResponse interpret(InterpretRequest request) {
		String text = request.text().trim();
		String lower = text.toLowerCase(Locale.ROOT);
		List<String> notes = new ArrayList<>();

		Station origin = null;
		Station destination = null;
		Matcher routeMatch = ROUTE.matcher(text);
		if (routeMatch.find()) {
			origin = firstMatch(routeMatch.group(1).trim());
			destination = firstMatch(routeMatch.group(2).trim());
			if (origin == null) {
				notes.add("Could not recognise the origin station");
			}
			if (destination == null) {
				notes.add("Could not recognise the destination station");
			}
		}
		else {
			notes.add("Say it as \"from <city> to <city>\" so the origin and destination are clear");
		}

		LocalDate date = parseDate(lower);
		if (date == null) {
			notes.add("No date recognised; defaulting to today");
			date = LocalDate.now();
		}

		Integer passengers = null;
		Matcher passengerMatch = PASSENGERS.matcher(lower);
		if (passengerMatch.find()) {
			passengers = Integer.parseInt(passengerMatch.group(1));
		}

		TransportPreference transport = lower.contains("train") ? TransportPreference.TRAIN
				: lower.contains("bus") ? TransportPreference.BUS : TransportPreference.ANY;

		RankingStrategy ranking = lower.contains("cheap") ? RankingStrategy.CHEAPEST
				: lower.contains("fast") || lower.contains("quick") ? RankingStrategy.FASTEST
						: lower.contains("comfort") ? RankingStrategy.COMFORTABLE
								: lower.contains("direct") || lower.contains("non-stop") || lower.contains("nonstop")
										? RankingStrategy.FEWEST_TRANSFERS
										: RankingStrategy.BALANCED;

		return new InterpretResponse(origin == null ? null : origin.getId(), origin == null ? null : origin.getName(),
				destination == null ? null : destination.getId(), destination == null ? null : destination.getName(),
				date, passengers, transport, ranking, notes);
	}

	private Station firstMatch(String text) {
		return stations.search(text.toLowerCase(Locale.ROOT), PageRequest.of(0, 1)).stream().findFirst().orElse(null);
	}

	private static LocalDate parseDate(String lower) {
		LocalDate today = LocalDate.now();
		if (lower.contains("today")) {
			return today;
		}
		if (lower.contains("tomorrow")) {
			return today.plusDays(1);
		}
		boolean next = lower.contains("next ");
		for (DayOfWeek day : DayOfWeek.values()) {
			String name = day.name().toLowerCase(Locale.ROOT);
			if (lower.contains(name) || lower.contains(name.substring(0, 3))) {
				LocalDate candidate = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(day));
				return next && candidate.equals(today) ? candidate.plusWeeks(1) : candidate;
			}
		}
		return null;
	}
}
