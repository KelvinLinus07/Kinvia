package com.smarttransit.smart_transit.network;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {

	@EntityGraph(attributePaths = { "coach", "coach.vehicle" })
	@Query("select s from Seat s where s.id = :id")
	Optional<Seat> findWithCoachById(@Param("id") Long id);
}
