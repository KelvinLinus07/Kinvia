package com.smarttransit.smart_transit.journey;

import java.math.BigDecimal;

import com.smarttransit.smart_transit.network.CoachClass;

/** Price and remaining seats for one travel class over one leg. */
public record ClassOffer(CoachClass coachClass, BigDecimal fare, int availableSeats) {
}
