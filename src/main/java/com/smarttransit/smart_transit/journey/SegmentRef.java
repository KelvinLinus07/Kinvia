package com.smarttransit.smart_transit.journey;

/** Identifies a leg: a trip plus the stop sequences where the traveller boards and alights. */
public record SegmentRef(long tripId, int boardSequence, int alightSequence) {
}
