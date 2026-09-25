package com.smarttransit.smart_transit.network;

/** Travel classes. {@code comfort} (0-100) feeds journey ranking; it is a product judgement, not a measurement. */
public enum CoachClass {
	SLEEPER(TransportMode.TRAIN, "Sleeper", 35),
	CHAIR_CAR(TransportMode.TRAIN, "Chair Car", 50),
	AC_3_TIER(TransportMode.TRAIN, "AC 3 Tier", 65),
	AC_2_TIER(TransportMode.TRAIN, "AC 2 Tier", 80),
	AC_1_TIER(TransportMode.TRAIN, "AC First Class", 95),
	SEATER(TransportMode.BUS, "Seater", 30),
	AC_SEATER(TransportMode.BUS, "AC Seater", 60),
	AC_SLEEPER(TransportMode.BUS, "AC Sleeper", 85);

	private final TransportMode mode;
	private final String label;
	private final int comfort;

	CoachClass(TransportMode mode, String label, int comfort) {
		this.mode = mode;
		this.label = label;
		this.comfort = comfort;
	}

	public TransportMode mode() { return mode; }

	public String label() { return label; }

	public int comfort() { return comfort; }
}
