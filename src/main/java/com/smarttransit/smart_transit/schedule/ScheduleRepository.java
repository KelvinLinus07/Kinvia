package com.smarttransit.smart_transit.schedule;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

	@EntityGraph(attributePaths = { "route", "vehicle" })
	List<Schedule> findByActiveTrue();

	@EntityGraph(attributePaths = { "route", "vehicle" })
	@Query("select s from Schedule s where (:operatorId is null or s.route.operator.id = :operatorId) order by s.route.name, s.departureTime")
	Page<Schedule> findScoped(@Param("operatorId") Long operatorId, Pageable pageable);
}
