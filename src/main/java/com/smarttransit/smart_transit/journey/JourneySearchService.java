package com.smarttransit.smart_transit.journey;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.config.KinviaProperties;
import com.smarttransit.smart_transit.journey.JourneyDtos.JourneyDto;
import com.smarttransit.smart_transit.journey.JourneyDtos.SearchResponse;
import com.smarttransit.smart_transit.network.NetworkDtos.StationDto;
import com.smarttransit.smart_transit.network.Station;
import com.smarttransit.smart_transit.network.StationRepository;
import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.recommendation.JourneyScorer;
import com.smarttransit.smart_transit.recommendation.RankingContext;
import com.smarttransit.smart_transit.recommendation.RankingStrategy;
import com.smarttransit.smart_transit.recommendation.ScoredItinerary;
import com.smarttransit.smart_transit.security.CurrentUser;
import com.smarttransit.smart_transit.timetable.TimetableActivator;
import com.smarttransit.smart_transit.user.TravelPreference;
import com.smarttransit.smart_transit.user.TravelPreferenceRepository;

@Service
public class JourneySearchService {

	/** Journeys much slower than the best option are noise, not alternatives. */
	private static final double MAX_DURATION_FACTOR = 2.5;
	private static final Duration MAX_EXTRA_DURATION = Duration.ofHours(8);

	private final StationRepository stations;
	private final LegCatalog legCatalog;
	private final JourneyScorer scorer;
	private final JourneyMapper mapper;
	private final TravelPreferenceRepository preferences;
	private final KinviaProperties properties;
	private final Clock clock;
	private final TimetableActivator timetable;
	private final TransactionTemplate readOnly;

	public JourneySearchService(StationRepository stations, LegCatalog legCatalog, JourneyScorer scorer,
			JourneyMapper mapper, TravelPreferenceRepository preferences, KinviaProperties properties, Clock clock,
			TimetableActivator timetable, PlatformTransactionManager transactionManager) {
		this.stations = stations;
		this.legCatalog = legCatalog;
		this.scorer = scorer;
		this.mapper = mapper;
		this.preferences = preferences;
		this.properties = properties;
		this.clock = clock;
		this.timetable = timetable;
		this.readOnly = new TransactionTemplate(transactionManager);
		this.readOnly.setReadOnly(true);
	}

	/**
	 * Imported timetable services are put into the operational network (in their own committed transaction) before
	 * the read-only search transaction opens, so the search always sees them.
	 */
	public SearchResponse search(SearchQuery query, CurrentUser user) {
		validate(query);
		timetable.activate(query.originId(), query.destinationId(), query.date());
		return readOnly.execute(status -> runSearch(query, user));
	}

	private SearchResponse runSearch(SearchQuery query, CurrentUser user) {
		TravelPreference preference = user == null ? null : preferences.findByUserId(user.id()).orElse(null);
		TransportPreference transport = firstNonNull(query.transport(),
				preference == null ? null : preference.getTransportPreference(), TransportPreference.ANY);
		RankingStrategy ranking = firstNonNull(query.ranking(), preference == null ? null : preference.getRanking(),
				RankingStrategy.BALANCED);
		int maxTransfers = Math.min(properties.journey().maxTransfers(), firstNonNull(query.maxTransfers(),
				preference == null ? null : preference.getMaxTransfers(), properties.journey().maxTransfers()));

		Station origin = station(query.originId());
		Station destination = station(query.destinationId());

		Set<TransportMode> modes = Arrays.stream(TransportMode.values()).filter(transport::allows)
				.collect(Collectors.toSet());
		List<Leg> legs = legCatalog.searchLegs(query.date(), modes, origin.getId(), destination.getId()).stream()
				.filter(leg -> leg.board().stationId() != origin.getId()
						|| leg.departure().toLocalDate().equals(query.date()))
				.toList();

		List<Itinerary> found = new ItineraryPlanner(maxTransfers, properties.journey().maxWait())
				.plan(legs, origin.getId(), destination.getId(), query.passengers());
		JourneyFilter filter = query.filter() == null ? JourneyFilter.NONE : query.filter();
		List<Itinerary> withoutLimits = prune(found);
		List<Itinerary> withinLimits = filter.isActive()
				? prune(found.stream().filter(i -> filter.accepts(i, query.passengers())).toList())
				: withoutLimits;
		List<ScoredItinerary> ranked = scorer.rank(withinLimits,
				new RankingContext(ranking, query.passengers())).stream()
				.limit(properties.journey().maxResults()).toList();

		List<JourneyDto> journeys = ranked.stream()
				.map(scored -> mapper.toDto(scored, query.passengers(), ranked.size())).toList();
		return new SearchResponse(StationDto.from(origin), StationDto.from(destination), query.date(),
				query.passengers(), transport, ranking, filter, withoutLimits.size(), journeys);
	}

	/** Re-derives a single journey from its key with current fares and availability. */
	@Transactional(readOnly = true)
	public JourneyDto details(String key, int passengers) {
		if (passengers < 1 || passengers > properties.booking().maxPassengers()) {
			throw ApiException.badRequest("Passengers must be between 1 and " + properties.booking().maxPassengers());
		}
		Itinerary itinerary = new Itinerary(legCatalog.resolve(JourneyKey.parse(key)));
		ScoredItinerary single = new ScoredItinerary(itinerary, 0, Set.of());
		return mapper.toDto(single, passengers, 1);
	}

	private List<Itinerary> prune(List<Itinerary> itineraries) {
		if (itineraries.isEmpty()) {
			return itineraries;
		}
		Duration best = itineraries.stream().map(Itinerary::duration).min(Duration::compareTo).orElseThrow();
		Duration limit = Duration.ofMinutes(Math.max((long) (best.toMinutes() * MAX_DURATION_FACTOR),
				best.plus(MAX_EXTRA_DURATION).toMinutes()));
		return itineraries.stream().filter(i -> i.duration().compareTo(limit) <= 0).toList();
	}

	private void validate(SearchQuery query) {
		if (query.originId() == query.destinationId()) {
			throw ApiException.badRequest("Origin and destination must be different");
		}
		LocalDate today = LocalDate.now(clock);
		if (query.date().isBefore(today) || query.date().isAfter(today.plusDays(properties.trips().horizonDays()))) {
			throw ApiException.badRequest("Choose a date between today and " + today.plusDays(properties.trips().horizonDays()));
		}
		if (query.passengers() < 1 || query.passengers() > properties.booking().maxPassengers()) {
			throw ApiException.badRequest("Passengers must be between 1 and " + properties.booking().maxPassengers());
		}
	}

	private Station station(long id) {
		return stations.findById(id).filter(Station::isActive).orElseThrow(() -> ApiException.notFound("Station", id));
	}

	@SafeVarargs
	private static <T> T firstNonNull(T... values) {
		for (T value : values) {
			if (value != null) {
				return value;
			}
		}
		throw new IllegalArgumentException("No value available");
	}
}
