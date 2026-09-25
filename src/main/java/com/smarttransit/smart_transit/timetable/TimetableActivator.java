package com.smarttransit.smart_transit.timetable;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.smarttransit.smart_transit.network.CoachClass;
import com.smarttransit.smart_transit.network.Operator;
import com.smarttransit.smart_transit.network.OperatorRepository;
import com.smarttransit.smart_transit.network.Route;
import com.smarttransit.smart_transit.network.RouteRepository;
import com.smarttransit.smart_transit.network.Station;
import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.network.Vehicle;
import com.smarttransit.smart_transit.network.VehicleRepository;
import com.smarttransit.smart_transit.schedule.Schedule;
import com.smarttransit.smart_transit.schedule.ScheduleRepository;
import com.smarttransit.smart_transit.schedule.Trip;
import com.smarttransit.smart_transit.schedule.TripRepository;

/**
 * Turns imported timetable rows into the operational records the rest of Kinvia works with (operator, vehicle with
 * seats, route, schedule and dated trips).
 * <p>
 * The workbook holds about 120,000 services. Creating a seat map for every one of them up front would mean millions
 * of rows nobody will ever book, so a pair of stations is activated the first time it is searched, and only the
 * trips for the searched date are created. After that the nightly {@code TripGenerator} keeps them rolling forward
 * like any other schedule.
 */
@Service
public class TimetableActivator {

	private record CoachLayout(String code, int rows, int columns, int aisleAfter) {
	}

	/**
	 * Seat layouts are a product choice (the workbook has none); most match the original development seed. AC First
	 * Class is new (the workbook's "Price of 1st AC" column) and gets the smallest layout, matching its real-world
	 * role as the most exclusive, lowest-capacity class.
	 */
	private static final Map<CoachClass, CoachLayout> LAYOUTS = Map.of(
			CoachClass.SLEEPER, new CoachLayout("S1", 8, 6, 3),
			CoachClass.AC_3_TIER, new CoachLayout("B1", 8, 6, 3),
			CoachClass.AC_2_TIER, new CoachLayout("A1", 6, 4, 2),
			CoachClass.AC_1_TIER, new CoachLayout("H1", 4, 4, 2),
			CoachClass.SEATER, new CoachLayout("MAIN", 10, 4, 2));

	private static final String TRAIN_OPERATOR_CODE = "DEMO-RAIL";
	private static final String TRAIN_OPERATOR_NAME = "Demo Rail Services";

	private final TimetableEntryRepository entries;
	private final OperatorRepository operators;
	private final VehicleRepository vehicles;
	private final RouteRepository routes;
	private final ScheduleRepository schedules;
	private final TripRepository trips;
	private final TransactionTemplate transaction;

	/** Held around the whole transaction so two identical first searches cannot both create the same service. */
	private final ReentrantLock lock = new ReentrantLock();

	public TimetableActivator(TimetableEntryRepository entries, OperatorRepository operators,
			VehicleRepository vehicles, RouteRepository routes, ScheduleRepository schedules, TripRepository trips,
			PlatformTransactionManager transactionManager) {
		this.entries = entries;
		this.operators = operators;
		this.vehicles = vehicles;
		this.routes = routes;
		this.schedules = schedules;
		this.trips = trips;
		this.transaction = new TransactionTemplate(transactionManager);
		this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	/** Makes sure every imported service between the two stations exists, with trips on {@code date}. Idempotent. */
	public void activate(long originId, long destinationId, LocalDate date) {
		lock.lock();
		try {
			transaction.executeWithoutResult(status -> activateInTransaction(originId, destinationId, date));
		} finally {
			lock.unlock();
		}
	}

	private void activateInTransaction(long originId, long destinationId, LocalDate date) {
		List<TimetableEntry> pair = entries.findForPair(originId, destinationId);
		for (TimetableEntry entry : pair) {
			if (!entry.isActivated()) {
				createService(entry);
			}
		}
		createMissingTrips(pair, date);
	}

	private void createService(TimetableEntry entry) {
		Station origin = entry.getOrigin();
		Station destination = entry.getDestination();
		Operator operator = operatorFor(entry);
		String number = serviceCode(entry);
		Map<CoachClass, Integer> fares = entry.offeredFares();

		Vehicle vehicle = new Vehicle(operator, number, entry.getName(), "");
		fares.keySet().forEach(coachClass -> {
			CoachLayout layout = LAYOUTS.get(coachClass);
			vehicle.addCoach(layout.code(), coachClass, layout.rows(), layout.columns(), layout.aisleAfter());
		});
		vehicles.save(vehicle);

		TravelEstimator.Estimate trip = TravelEstimator.estimate(entry.getMode(), origin.getLatitude(),
				origin.getLongitude(), destination.getLatitude(), destination.getLongitude());
		Route route = new Route(operator, number, origin.getName() + " to " + destination.getName());
		route.addStop(origin, 0, 0, 0);
		route.addStop(destination, trip.minutes(), trip.minutes(), trip.distanceKm());
		// The workbook gives one flat fare per class, so the per-km rate is zero and the fare is the minimum.
		fares.forEach((coachClass, fare) -> route.setFare(coachClass, BigDecimal.ZERO, BigDecimal.valueOf(fare)));
		routes.save(route);

		Schedule schedule = schedules.save(
				new Schedule(route, vehicle, entry.getDepartureTime(), EnumSet.allOf(DayOfWeek.class)));
		entry.markActivated(route, schedule);
	}

	private void createMissingTrips(List<TimetableEntry> pair, LocalDate date) {
		List<Long> scheduleIds = pair.stream().map(e -> e.getSchedule().getId()).toList();
		if (scheduleIds.isEmpty()) {
			return;
		}
		Set<Long> alreadyScheduled = new HashSet<>(trips.findScheduledIdsAmong(date, scheduleIds));
		for (TimetableEntry entry : pair) {
			if (!alreadyScheduled.contains(entry.getSchedule().getId())) {
				trips.save(new Trip(entry.getSchedule(), date));
			}
		}
	}

	/** Trains share one operator (the workbook names none); each bus brand in the workbook is its own operator. */
	private Operator operatorFor(TimetableEntry entry) {
		if (entry.getMode() == TransportMode.TRAIN) {
			return operator(TRAIN_OPERATOR_CODE, TRAIN_OPERATOR_NAME, TransportMode.TRAIN);
		}
		String brand = entry.getName().replaceFirst("\\s*\\d+\\s*$", "");
		return operator("BUS-" + TimetableCodes.initials(brand), brand, TransportMode.BUS);
	}

	private Operator operator(String code, String name, TransportMode mode) {
		return operators.findByCodeIgnoreCase(code)
				.orElseGet(() -> operators.save(new Operator(code, name, mode, null)));
	}

	static String serviceCode(TimetableEntry entry) {
		return TimetableCodes.serviceCode(() -> entry.getMode() == TransportMode.TRAIN, entry.getServiceNumber());
	}
}
