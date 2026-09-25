package com.smarttransit.smart_transit.timetable;

/** Pure naming rules for an imported service: the vehicle/route code and the operator initials for a bus brand. */
final class TimetableCodes {

	private TimetableCodes() {
	}

	/** "T00042" / "B00007": unique across both modes, used as vehicle number and route code. */
	static String serviceCode(TransportModeLike mode, int serviceNumber) {
		return (mode.isTrain() ? "T" : "B") + String.format("%05d", serviceNumber);
	}

	/** "Eastern Star Travels" becomes "EST". */
	static String initials(String brand) {
		StringBuilder initials = new StringBuilder();
		for (String word : brand.trim().split("\\s+")) {
			initials.append(Character.toUpperCase(word.charAt(0)));
		}
		return initials.toString();
	}

	/** Avoids a compile dependency on the network module's TransportMode from this small pure helper. */
	interface TransportModeLike {
		boolean isTrain();
	}
}
