package com.smarttransit.smart_transit.schedule;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smarttransit.smart_transit.network.TransportMode;

import jakarta.persistence.LockModeType;

public interface TripRepository extends JpaRepository<Trip, Long> {

	/** Serialises seat allocation for one trip: concurrent holds queue up on this row lock. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Trip t where t.id = :id")
	Optional<Trip> lockById(@Param("id") Long id);

	@EntityGraph(attributePaths = { "route", "route.operator", "vehicle" })
	@Query("select t from Trip t where t.id = :id")
	Optional<Trip> findDetailedById(@Param("id") Long id);

	@EntityGraph(attributePaths = { "route", "route.operator", "vehicle" })
	@Query("select t from Trip t where t.id in :ids")
	List<Trip> findDetailedByIds(@Param("ids") Collection<Long> ids);

	/**
	 * Trips a search for {@code date} could use. Routes that belong to the imported timetable are point-to-point
	 * services, so they are only offered when they run straight from the origin to the destination; every other
	 * route is available as a possible leg of a connection.
	 */
	@EntityGraph(attributePaths = { "route", "route.operator", "vehicle" })
	@Query("""
			select t from Trip t
			where t.serviceDate between :from and :to and t.status in (com.smarttransit.smart_transit.schedule.TripStatus.SCHEDULED,
			  com.smarttransit.smart_transit.schedule.TripStatus.DELAYED)
			  and t.route.active = true and t.vehicle.status = com.smarttransit.smart_transit.network.VehicleStatus.ACTIVE
			  and t.route.mode in :modes
			  and (not exists (select 1 from TimetableEntry e where e.route.id = t.route.id)
			    or exists (select 1 from RouteStop o, RouteStop d
			      where o.route.id = t.route.id and d.route.id = t.route.id
			        and o.station.id = :originId and d.station.id = :destinationId and o.sequence < d.sequence))
			""")
	List<Trip> findBookableBetween(@Param("from") LocalDate from, @Param("to") LocalDate to,
			@Param("modes") Collection<TransportMode> modes, @Param("originId") long originId,
			@Param("destinationId") long destinationId);

	@EntityGraph(attributePaths = { "route", "route.operator", "vehicle" })
	@Query(value = """
			select t from Trip t
			where (:operatorId is null or t.route.operator.id = :operatorId) and t.serviceDate = :date
			order by t.departure
			""", countQuery = """
			select count(t) from Trip t
			where (:operatorId is null or t.route.operator.id = :operatorId) and t.serviceDate = :date
			""")
	Page<Trip> findScopedOnDate(@Param("operatorId") Long operatorId, @Param("date") LocalDate date, Pageable pageable);

	@Query("select t.schedule.id from Trip t where t.serviceDate = :date")
	List<Long> findScheduledIdsOn(@Param("date") LocalDate date);

	@Query("select t.schedule.id from Trip t where t.serviceDate = :date and t.schedule.id in :scheduleIds")
	List<Long> findScheduledIdsAmong(@Param("date") LocalDate date, @Param("scheduleIds") Collection<Long> scheduleIds);

	boolean existsByRouteId(Long routeId);

	long countByServiceDateAndStatusIn(LocalDate date, Collection<TripStatus> statuses);
}
