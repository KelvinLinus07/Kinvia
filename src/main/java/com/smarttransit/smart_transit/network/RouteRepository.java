package com.smarttransit.smart_transit.network;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteRepository extends JpaRepository<Route, Long> {

	boolean existsByCodeIgnoreCase(String code);

	@EntityGraph(attributePaths = { "operator" })
	@Query("select r from Route r where (:operatorId is null or r.operator.id = :operatorId) order by r.name")
	Page<Route> findScoped(@Param("operatorId") Long operatorId, Pageable pageable);

	@Query("select rs from RouteStop rs join fetch rs.station where rs.route.id in :routeIds order by rs.route.id, rs.sequence")
	List<RouteStop> findStops(@Param("routeIds") Collection<Long> routeIds);

	@Query("select f from RouteFare f where f.route.id in :routeIds")
	List<RouteFare> findFares(@Param("routeIds") Collection<Long> routeIds);
}
