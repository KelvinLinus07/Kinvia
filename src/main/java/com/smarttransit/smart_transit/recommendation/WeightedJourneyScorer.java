package com.smarttransit.smart_transit.recommendation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import org.springframework.stereotype.Component;

import com.smarttransit.smart_transit.journey.Itinerary;

/**
 * Min-max normalises each criterion across the candidate set, then combines them with the strategy's weights.
 * Itineraries that cannot seat the whole party always rank after those that can.
 */
@Component
public class WeightedJourneyScorer implements JourneyScorer {

	@Override
	public List<ScoredItinerary> rank(List<Itinerary> candidates, RankingContext context) {
		if (candidates.isEmpty()) {
			return List.of();
		}
		int pax = context.passengers();
		ScoreWeights weights = ScoreWeights.forStrategy(context.strategy());
		Range duration = Range.of(candidates, i -> i.duration().toMinutes());
		Range price = Range.of(candidates, i -> i.farePerPassenger(pax).doubleValue());
		Range transfers = Range.of(candidates, Itinerary::transfers);
		Range comfort = Range.of(candidates, i -> i.comfortFor(pax));
		Range waiting = Range.of(candidates, i -> i.waiting().toMinutes());

		List<ScoredItinerary> ranked = new ArrayList<>();
		for (Itinerary itinerary : candidates) {
			double cost = weights.duration() * duration.normalise(itinerary.duration().toMinutes())
					+ weights.price() * price.normalise(itinerary.farePerPassenger(pax).doubleValue())
					+ weights.transfers() * transfers.normalise(itinerary.transfers())
					+ weights.comfort() * (1 - comfort.normalise(itinerary.comfortFor(pax)))
					+ weights.waiting() * waiting.normalise(itinerary.waiting().toMinutes());
			double score = Math.round(1000 * (1 - cost / weights.total())) / 10.0;
			ranked.add(new ScoredItinerary(itinerary, score, EnumSet.noneOf(JourneyTag.class)));
		}
		ranked.sort(Comparator.comparing((ScoredItinerary s) -> !s.itinerary().isBookableFor(pax))
				.thenComparing(Comparator.comparingDouble(ScoredItinerary::score).reversed())
				.thenComparing(s -> s.itinerary().departure()));
		return tag(ranked, pax, duration, price, transfers, comfort);
	}

	private List<ScoredItinerary> tag(List<ScoredItinerary> ranked, int pax, Range duration, Range price,
			Range transfers, Range comfort) {
		boolean several = ranked.size() > 1;
		List<ScoredItinerary> tagged = new ArrayList<>();
		for (int i = 0; i < ranked.size(); i++) {
			Itinerary itinerary = ranked.get(i).itinerary();
			Set<JourneyTag> tags = EnumSet.noneOf(JourneyTag.class);
			if (i == 0 && itinerary.isBookableFor(pax)) {
				tags.add(JourneyTag.RECOMMENDED);
			}
			if (several && itinerary.isBookableFor(pax)) {
				addIf(tags, JourneyTag.FASTEST, duration.isBest(itinerary.duration().toMinutes()), duration);
				addIf(tags, JourneyTag.CHEAPEST, price.isBest(itinerary.farePerPassenger(pax).doubleValue()), price);
				addIf(tags, JourneyTag.FEWEST_TRANSFERS, transfers.isBest(itinerary.transfers()), transfers);
				addIf(tags, JourneyTag.MOST_COMFORTABLE, comfort.isHighest(itinerary.comfortFor(pax)), comfort);
			}
			tagged.add(new ScoredItinerary(itinerary, ranked.get(i).score(), tags));
		}
		return tagged;
	}

	/** A tag is only meaningful when the criterion actually differs between candidates. */
	private static void addIf(Set<JourneyTag> tags, JourneyTag tag, boolean isBest, Range range) {
		if (isBest && range.max > range.min) {
			tags.add(tag);
		}
	}

	private record Range(double min, double max) {

		static Range of(List<Itinerary> items, ToDoubleFunction<Itinerary> metric) {
			double min = Double.MAX_VALUE;
			double max = -Double.MAX_VALUE;
			for (Itinerary item : items) {
				double value = metric.applyAsDouble(item);
				min = Math.min(min, value);
				max = Math.max(max, value);
			}
			return new Range(min, max);
		}

		/** 0 for the best (lowest) value, 1 for the worst. */
		double normalise(double value) {
			return max == min ? 0 : (value - min) / (max - min);
		}

		boolean isBest(double value) {
			return Math.abs(value - min) < 1e-9;
		}

		boolean isHighest(double value) {
			return Math.abs(value - max) < 1e-9;
		}
	}
}
