package com.smarttransit.smart_transit.network;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

	boolean existsByNumberIgnoreCase(String number);

	@EntityGraph(attributePaths = { "operator", "coaches" })
	@Query("select v from Vehicle v where v.id in :ids")
	List<Vehicle> findAllWithCoaches(@Param("ids") Collection<Long> ids);

	@Query(value = """
			select v.id from Vehicle v
			where (:operatorId is null or v.operator.id = :operatorId) and (:mode is null or v.mode = :mode)
			order by v.name
			""", countQuery = """
			select count(v) from Vehicle v
			where (:operatorId is null or v.operator.id = :operatorId) and (:mode is null or v.mode = :mode)
			""")
	Page<Long> findIds(@Param("operatorId") Long operatorId, @Param("mode") TransportMode mode, Pageable pageable);
}
