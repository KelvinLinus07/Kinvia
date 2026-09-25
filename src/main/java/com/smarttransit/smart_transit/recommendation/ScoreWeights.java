package com.smarttransit.smart_transit.recommendation;

/** How much each criterion matters for a strategy. Weights need not sum to one; they are normalised on use. */
public record ScoreWeights(double duration, double price, double transfers, double comfort, double waiting) {

	public static ScoreWeights forStrategy(RankingStrategy strategy) {
		return switch (strategy) {
			case FASTEST -> new ScoreWeights(0.70, 0.10, 0.10, 0.00, 0.10);
			case CHEAPEST -> new ScoreWeights(0.10, 0.80, 0.10, 0.00, 0.00);
			case FEWEST_TRANSFERS -> new ScoreWeights(0.25, 0.00, 0.60, 0.00, 0.15);
			case COMFORTABLE -> new ScoreWeights(0.10, 0.00, 0.20, 0.60, 0.10);
			case BALANCED -> new ScoreWeights(0.30, 0.25, 0.20, 0.15, 0.10);
		};
	}

	double total() {
		return duration + price + transfers + comfort + waiting;
	}
}
