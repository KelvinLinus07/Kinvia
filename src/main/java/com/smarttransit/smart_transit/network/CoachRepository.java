package com.smarttransit.smart_transit.network;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoachRepository extends JpaRepository<Coach, Long> {

	@Query("select distinct c from Coach c join fetch c.seats where c.vehicle.id = :vehicleId order by c.code")
	List<Coach> findWithSeatsByVehicleId(@Param("vehicleId") Long vehicleId);
}
