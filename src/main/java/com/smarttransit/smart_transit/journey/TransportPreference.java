package com.smarttransit.smart_transit.journey;

import com.smarttransit.smart_transit.network.TransportMode;

public enum TransportPreference {
	ANY, TRAIN, BUS;

	public boolean allows(TransportMode mode) {
		return this == ANY || this.name().equals(mode.name());
	}
}
