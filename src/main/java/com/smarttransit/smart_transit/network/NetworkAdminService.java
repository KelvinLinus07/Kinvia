package com.smarttransit.smart_transit.network;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.common.PageResponse;
import com.smarttransit.smart_transit.network.NetworkDtos.CoachRequest;
import com.smarttransit.smart_transit.network.NetworkDtos.FareRequest;
import com.smarttransit.smart_transit.network.NetworkDtos.OperatorDto;
import com.smarttransit.smart_transit.network.NetworkDtos.OperatorRequest;
import com.smarttransit.smart_transit.network.NetworkDtos.RouteDto;
import com.smarttransit.smart_transit.network.NetworkDtos.RouteRequest;
import com.smarttransit.smart_transit.network.NetworkDtos.RouteStopRequest;
import com.smarttransit.smart_transit.network.NetworkDtos.StationDto;
import com.smarttransit.smart_transit.network.NetworkDtos.StationRequest;
import com.smarttransit.smart_transit.network.NetworkDtos.VehicleDto;
import com.smarttransit.smart_transit.network.NetworkDtos.VehicleRequest;
import com.smarttransit.smart_transit.schedule.TripRepository;
import com.smarttransit.smart_transit.security.CurrentUser;

/** Fleet, route and station management shared by the admin (unrestricted) and operator (scoped) dashboards. */
@Service
public class NetworkAdminService {

	private final StationRepository stations;
	private final OperatorRepository operators;
	private final VehicleRepository vehicles;
	private final RouteRepository routes;
	private final TripRepository trips;

	public NetworkAdminService(StationRepository stations, OperatorRepository operators, VehicleRepository vehicles,
			RouteRepository routes, TripRepository trips) {
		this.stations = stations;
		this.operators = operators;
		this.vehicles = vehicles;
		this.routes = routes;
		this.trips = trips;
	}

	@Transactional
	public StationDto createStation(StationRequest request) {
		if (stations.existsByCodeIgnoreCase(request.code())) {
			throw ApiException.conflict("Station code " + request.code() + " is already in use");
		}
		Station station = new Station(request.code(), request.name(), request.city(), request.state(), request.kind(),
				request.latitude(), request.longitude(), request.transferMinutes());
		return StationDto.from(stations.save(station));
	}

	@Transactional
	public StationDto updateStation(Long id, StationRequest request) {
		Station station = stations.findById(id).orElseThrow(() -> ApiException.notFound("Station", id));
		station.update(request.code(), request.name(), request.city(), request.state(), request.kind(),
				request.latitude(), request.longitude(), request.transferMinutes(),
				request.active() == null || request.active());
		return StationDto.from(station);
	}

	@Transactional
	public OperatorDto createOperator(OperatorRequest request) {
		if (operators.existsByCodeIgnoreCase(request.code())) {
			throw ApiException.conflict("Operator code " + request.code() + " is already in use");
		}
		return OperatorDto.from(operators.save(new Operator(request.code(), request.name(), request.mode(),
				request.contactEmail())));
	}

	@Transactional
	public OperatorDto updateOperator(Long id, OperatorRequest request) {
		Operator operator = operators.findById(id).orElseThrow(() -> ApiException.notFound("Operator", id));
		operator.update(request.code(), request.name(), request.mode(), request.contactEmail(),
				request.active() == null || request.active());
		return OperatorDto.from(operator);
	}

	@Transactional(readOnly = true)
	public List<OperatorDto> listOperators() {
		return operators.findAll().stream().map(OperatorDto::from).toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<VehicleDto> listVehicles(CurrentUser caller, TransportMode mode, int page, int size) {
		var ids = vehicles.findIds(caller.operatorScope(), mode, PageRequest.of(page, size));
		List<VehicleDto> content = vehicles.findAllWithCoaches(ids.getContent()).stream().map(VehicleDto::from).toList();
		return new PageResponse<>(content, ids.getNumber(), ids.getSize(), ids.getTotalElements(), ids.getTotalPages());
	}

	private Vehicle loadedVehicle(Long id) {
		return vehicles.findAllWithCoaches(List.of(id)).stream().findFirst()
				.orElseThrow(() -> ApiException.notFound("Vehicle", id));
	}

	@Transactional
	public VehicleDto createVehicle(CurrentUser caller, VehicleRequest request) {
		Long operatorId = resolveOperatorId(caller, request.operatorId());
		if (vehicles.existsByNumberIgnoreCase(request.number())) {
			throw ApiException.conflict("Vehicle number " + request.number() + " is already registered");
		}
		Operator operator = operators.findById(operatorId).orElseThrow(() -> ApiException.notFound("Operator", operatorId));
		Vehicle vehicle = new Vehicle(operator, request.number(), request.name(), request.amenities());
		addCoaches(vehicle, request.coaches());
		return VehicleDto.from(vehicles.save(vehicle));
	}

	@Transactional
	public VehicleDto updateVehicle(CurrentUser caller, Long id, VehicleRequest request) {
		Vehicle vehicle = loadedVehicle(id);
		caller.requireAccessTo(vehicle.getOperator().getId());
		vehicle.update(request.name(), request.status() == null ? vehicle.getStatus() : request.status(),
				request.amenities());
		return VehicleDto.from(vehicle);
	}

	private void addCoaches(Vehicle vehicle, List<CoachRequest> requests) {
		if (requests == null) {
			return;
		}
		Set<String> codes = new java.util.HashSet<>();
		for (CoachRequest coach : requests) {
			if (coach.coachClass().mode() != vehicle.getMode()) {
				throw ApiException.badRequest(coach.coachClass() + " cannot be used on a " + vehicle.getMode() + " vehicle");
			}
			if (!codes.add(coach.code().toUpperCase())) {
				throw ApiException.badRequest("Duplicate coach code " + coach.code());
			}
			vehicle.addCoach(coach.code(), coach.coachClass(), coach.rows(), coach.columns(), coach.aisleAfter());
		}
	}

	@Transactional(readOnly = true)
	public PageResponse<RouteDto> listRoutes(CurrentUser caller, int page, int size) {
		return PageResponse.of(routes.findScoped(caller.operatorScope(), PageRequest.of(page, size)), RouteDto::from);
	}

	@Transactional
	public RouteDto createRoute(CurrentUser caller, RouteRequest request) {
		Long operatorId = resolveOperatorId(caller, request.operatorId());
		if (routes.existsByCodeIgnoreCase(request.code())) {
			throw ApiException.conflict("Route code " + request.code() + " is already in use");
		}
		Operator operator = operators.findById(operatorId).orElseThrow(() -> ApiException.notFound("Operator", operatorId));
		Route route = new Route(operator, request.code(), request.name());
		applyStopsAndFares(route, request.stops(), request.fares());
		return RouteDto.from(routes.save(route));
	}

	@Transactional
	public RouteDto updateRoute(CurrentUser caller, Long id, RouteRequest request) {
		Route route = routes.findById(id).orElseThrow(() -> ApiException.notFound("Route", id));
		caller.requireAccessTo(route.getOperator().getId());
		if (trips.existsByRouteId(id)) {
			throw ApiException.conflict("This route already has scheduled trips; create a new route instead of changing its stops");
		}
		route.rename(request.code(), request.name(), request.active() == null || request.active());
		route.clearStops();
		applyStopsAndFares(route, request.stops(), request.fares());
		return RouteDto.from(route);
	}

	private void applyStopsAndFares(Route route, List<RouteStopRequest> stopRequests, List<FareRequest> fareRequests) {
		int previousOffset = -1;
		for (RouteStopRequest stop : stopRequests) {
			if (stop.arrivalOffsetMinutes() <= previousOffset || stop.departureOffsetMinutes() < stop.arrivalOffsetMinutes()) {
				throw ApiException.badRequest("Stop times must increase along the route");
			}
			previousOffset = stop.departureOffsetMinutes();
			Station station = stations.findById(stop.stationId())
					.orElseThrow(() -> ApiException.notFound("Station", stop.stationId()));
			route.addStop(station, stop.arrivalOffsetMinutes(), stop.departureOffsetMinutes(), stop.distanceKm());
		}
		Set<CoachClass> allowedClasses = java.util.EnumSet.noneOf(CoachClass.class);
		for (FareRequest fare : fareRequests) {
			if (fare.coachClass().mode() != route.getMode()) {
				throw ApiException.badRequest(fare.coachClass() + " cannot be priced on a " + route.getMode() + " route");
			}
			route.setFare(fare.coachClass(), fare.ratePerKm(), fare.minimumFare());
			allowedClasses.add(fare.coachClass());
		}
		route.retainFares(allowedClasses);
	}

	private Long resolveOperatorId(CurrentUser caller, Long requestedOperatorId) {
		if (caller.isAdmin()) {
			if (requestedOperatorId == null) {
				throw ApiException.badRequest("Choose an operator");
			}
			return requestedOperatorId;
		}
		return caller.operatorScope();
	}
}
