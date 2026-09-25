package com.smarttransit.smart_transit.seed;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.config.KinviaProperties;
import com.smarttransit.smart_transit.network.CoachClass;
import com.smarttransit.smart_transit.network.Operator;
import com.smarttransit.smart_transit.network.OperatorRepository;
import com.smarttransit.smart_transit.network.Route;
import com.smarttransit.smart_transit.network.RouteRepository;
import com.smarttransit.smart_transit.network.Station;
import com.smarttransit.smart_transit.network.StationKind;
import com.smarttransit.smart_transit.network.StationRepository;
import com.smarttransit.smart_transit.network.TransportMode;
import com.smarttransit.smart_transit.network.Vehicle;
import com.smarttransit.smart_transit.network.VehicleRepository;
import com.smarttransit.smart_transit.schedule.Schedule;
import com.smarttransit.smart_transit.schedule.ScheduleRepository;
import com.smarttransit.smart_transit.schedule.TripGenerator;
import com.smarttransit.smart_transit.security.PasswordHasher;
import com.smarttransit.smart_transit.user.Role;
import com.smarttransit.smart_transit.user.TravelPreference;
import com.smarttransit.smart_transit.user.TravelPreferenceRepository;
import com.smarttransit.smart_transit.user.User;
import com.smarttransit.smart_transit.user.UserRepository;

/**
 * Populates a fresh database with clearly-marked development data: enough stations, operators, vehicles and routes
 * to demonstrate a direct train, a direct bus and a bus-to-train-to-bus multimodal journey, at different fares and
 * schedules. Runs only when {@code kinvia.seed.enabled=true} (the default) and only against an empty database, so
 * it never touches data from a previous run.
 */
@Component
@Order(100)
public class DevelopmentDataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DevelopmentDataSeeder.class);

	private final KinviaProperties properties;
	private final StationRepository stations;
	private final OperatorRepository operators;
	private final VehicleRepository vehicles;
	private final RouteRepository routes;
	private final ScheduleRepository schedules;
	private final UserRepository users;
	private final TravelPreferenceRepository preferences;
	private final PasswordHasher passwordHasher;
	private final TripGenerator tripGenerator;

	public DevelopmentDataSeeder(KinviaProperties properties, StationRepository stations,
			OperatorRepository operators, VehicleRepository vehicles, RouteRepository routes,
			ScheduleRepository schedules, UserRepository users, TravelPreferenceRepository preferences,
			PasswordHasher passwordHasher, TripGenerator tripGenerator) {
		this.properties = properties;
		this.stations = stations;
		this.operators = operators;
		this.vehicles = vehicles;
		this.routes = routes;
		this.schedules = schedules;
		this.users = users;
		this.preferences = preferences;
		this.passwordHasher = passwordHasher;
		this.tripGenerator = tripGenerator;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (!properties.seed().enabled()) {
			return;
		}
		if (stations.count() > 0 || users.count() > 0) {
			log.info("[DEV SEED] Existing data found; skipping development seed.");
			return;
		}
		log.info("[DEV SEED] Seeding development stations, operators, fleet, routes, schedules and accounts.");

		Map<String, Station> stationByCode = seedStations();
		Operator railOperator = operators.save(new Operator("NEC", "Northeast Corridor Railways", TransportMode.TRAIN,
				"ops@necrail.dev"));
		Operator busOperatorA = operators.save(new Operator("HMT", "Himalayan Motor Transport", TransportMode.BUS,
				"ops@hmtbus.dev"));
		Operator busOperatorB = operators.save(new Operator("GVL", "Ganga Valley Lines", TransportMode.BUS,
				"ops@gangavalley.dev"));

		seedDirectTrain(stationByCode, railOperator);
		seedDirectBus(stationByCode, busOperatorA);
		seedMultimodalChain(stationByCode, busOperatorB, railOperator, busOperatorA);

		seedAccounts(busOperatorA);

		tripGenerator.generate(LocalDate.now().minusDays(properties.trips().historyDays()),
				LocalDate.now().plusDays(properties.trips().horizonDays()));
		log.info("[DEV SEED] Complete. Sample accounts are listed in the README.");
	}

	private Map<String, Station> seedStations() {
		record S(String code, String name, String city, String state, StationKind kind, double lat, double lng) {
		}
		List<S> defs = List.of(
				new S("SGUJ", "Siliguri Junction", "Siliguri", "West Bengal", StationKind.HUB, 26.7271, 88.3953),
				new S("NJP", "New Jalpaiguri", "Siliguri", "West Bengal", StationKind.RAIL, 26.6950, 88.4102),
				new S("PNBE", "Patna Junction", "Patna", "Bihar", StationKind.RAIL, 25.6093, 85.1376),
				new S("PATB", "Patna Bus Terminus", "Patna", "Bihar", StationKind.BUS, 25.6141, 85.1414),
				new S("GAYB", "Gaya Bus Stand", "Gaya", "Bihar", StationKind.BUS, 24.7955, 84.9994),
				new S("HWH", "Howrah Junction", "Kolkata", "West Bengal", StationKind.RAIL, 22.5839, 88.3425),
				new S("DBRG", "Dibrugarh", "Dibrugarh", "Assam", StationKind.RAIL, 27.4728, 94.9120),
				new S("GHY", "Guwahati", "Guwahati", "Assam", StationKind.RAIL, 26.1445, 91.7362));
		return defs.stream().collect(java.util.stream.Collectors.toMap(S::code,
				d -> stations.save(new Station(d.code(), d.name(), d.city(), d.state(), d.kind(), d.lat(), d.lng(), 30))));
	}

	private void seedDirectTrain(Map<String, Station> s, Operator railOperator) {
		Vehicle train = vehicles.save(new Vehicle(railOperator, "12345", "Brahmaputra Mail",
				"Charging points,Pantry car,Blankets"));
		train.addCoach("S1", CoachClass.SLEEPER, 8, 6, 3);
		train.addCoach("B1", CoachClass.AC_3_TIER, 8, 6, 3);
		train.addCoach("A1", CoachClass.AC_2_TIER, 6, 4, 2);
		vehicles.save(train);

		Route route = routes.save(new Route(railOperator, "NEC-101", "Guwahati - Howrah Express"));
		route.addStop(s.get("GHY"), 0, 15, 0);
		route.addStop(s.get("NJP"), 300, 320, 470);
		route.addStop(s.get("HWH"), 780, 800, 970);
		route.setFare(CoachClass.SLEEPER, new BigDecimal("0.85"), new BigDecimal("150"));
		route.setFare(CoachClass.AC_3_TIER, new BigDecimal("1.60"), new BigDecimal("350"));
		route.setFare(CoachClass.AC_2_TIER, new BigDecimal("2.30"), new BigDecimal("550"));
		routes.save(route);

		schedules.save(new Schedule(route, train, LocalTime.of(21, 30),
				EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)));
	}

	private void seedDirectBus(Map<String, Station> s, Operator busOperator) {
		Vehicle bus = vehicles.save(new Vehicle(busOperator, "WB-74-A-1102", "Himalayan Volvo Multi-Axle",
				"WiFi,Charging points,Water bottle"));
		bus.addCoach("MAIN", CoachClass.AC_SLEEPER, 10, 3, 2);
		vehicles.save(bus);

		Route route = routes.save(new Route(busOperator, "HMT-7", "Siliguri - Patna Highway Express"));
		route.addStop(s.get("SGUJ"), 0, 15, 0);
		route.addStop(s.get("PATB"), 480, 490, 460);
		route.setFare(CoachClass.AC_SLEEPER, new BigDecimal("2.10"), new BigDecimal("400"));
		routes.save(route);

		schedules.save(new Schedule(route, bus, LocalTime.of(20, 0), EnumSet.allOf(DayOfWeek.class)));
	}

	private void seedMultimodalChain(Map<String, Station> s, Operator feederBusOperator, Operator railOperator,
			Operator lastMileBusOperator) {
		// Leg 1: local bus Siliguri Junction -> NJP (feeds the mainline train station)
		Vehicle feederBus = vehicles.save(new Vehicle(feederBusOperator, "WB-74-B-0090", "NJP Shuttle",
				"Charging points"));
		feederBus.addCoach("MAIN", CoachClass.AC_SEATER, 10, 4, 2);
		vehicles.save(feederBus);
		Route feederRoute = routes.save(new Route(feederBusOperator, "GVL-1", "Siliguri City - NJP Shuttle"));
		feederRoute.addStop(s.get("SGUJ"), 0, 10, 0);
		feederRoute.addStop(s.get("NJP"), 40, 45, 12);
		feederRoute.setFare(CoachClass.AC_SEATER, new BigDecimal("2.50"), new BigDecimal("40"));
		routes.save(feederRoute);
		schedules.save(new Schedule(feederRoute, feederBus, LocalTime.of(18, 30), EnumSet.allOf(DayOfWeek.class)));

		// Leg 2: mainline train NJP -> Patna Junction
		Vehicle mainlineTrain = vehicles.save(new Vehicle(railOperator, "12346", "Avadh Assam Express",
				"Pantry car,Charging points"));
		mainlineTrain.addCoach("S1", CoachClass.SLEEPER, 8, 6, 3);
		mainlineTrain.addCoach("B1", CoachClass.AC_3_TIER, 8, 6, 3);
		vehicles.save(mainlineTrain);
		Route mainlineRoute = routes.save(new Route(railOperator, "NEC-205", "NJP - Patna Junction Mail"));
		mainlineRoute.addStop(s.get("NJP"), 0, 20, 0);
		mainlineRoute.addStop(s.get("PNBE"), 480, 495, 500);
		mainlineRoute.setFare(CoachClass.SLEEPER, new BigDecimal("0.80"), new BigDecimal("120"));
		mainlineRoute.setFare(CoachClass.AC_3_TIER, new BigDecimal("1.55"), new BigDecimal("300"));
		routes.save(mainlineRoute);
		schedules.save(new Schedule(mainlineRoute, mainlineTrain, LocalTime.of(20, 15), EnumSet.allOf(DayOfWeek.class)));

		// Leg 3: last-mile bus Patna Junction area -> Gaya
		Vehicle lastMileBus = vehicles.save(new Vehicle(lastMileBusOperator, "BR-01-C-4455", "Patna - Gaya Connector",
				"Charging points"));
		lastMileBus.addCoach("MAIN", CoachClass.SEATER, 10, 4, 2);
		vehicles.save(lastMileBus);
		Route lastMileRoute = routes.save(new Route(lastMileBusOperator, "HMT-22", "Patna - Gaya Connector"));
		lastMileRoute.addStop(s.get("PATB"), 0, 15, 0);
		lastMileRoute.addStop(s.get("GAYB"), 150, 155, 100);
		lastMileRoute.setFare(CoachClass.SEATER, new BigDecimal("1.20"), new BigDecimal("60"));
		routes.save(lastMileRoute);
		schedules.save(new Schedule(lastMileRoute, lastMileBus, LocalTime.of(10, 0), EnumSet.allOf(DayOfWeek.class)));
	}

	private void seedAccounts(Operator sampleOperator) {
		String password = properties.seed().password();
		String hash = passwordHasher.hash(password);

		User admin = users.save(new User("admin@kinvia.dev", hash, "Kinvia Admin", "9000000001", Role.ADMIN, null));
		preferences.save(new TravelPreference(admin));

		User operatorUser = users.save(new User("operator@kinvia.dev", hash, "Himalayan Motor Transport Desk",
				"9000000002", Role.OPERATOR, sampleOperator));
		preferences.save(new TravelPreference(operatorUser));

		User passenger = users.save(new User("traveller@kinvia.dev", hash, "Asha Traveller", "9000000003",
				Role.PASSENGER, null));
		preferences.save(new TravelPreference(passenger));

		log.info("[DEV SEED] Sample accounts (password '{}'): admin@kinvia.dev, operator@kinvia.dev, traveller@kinvia.dev",
				password);
	}
}
