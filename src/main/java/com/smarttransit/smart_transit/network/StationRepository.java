package com.smarttransit.smart_transit.network;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StationRepository extends JpaRepository<Station, Long> {

	Optional<Station> findByCodeIgnoreCase(String code);

	Optional<Station> findFirstByNameIgnoreCase(String name);

	boolean existsByCodeIgnoreCase(String code);

	List<Station> findByActiveTrueOrderByName();

	/** Matches name, city or code anywhere in the text; an exact code, then names starting with the text, come first. */
	@Query("""
			select s from Station s
			where s.active = true and (lower(s.name) like concat('%', :q, '%')
			  or lower(s.city) like concat('%', :q, '%') or lower(s.code) like concat('%', :q, '%'))
			order by case when lower(s.code) = :q then 0
			              when lower(s.name) like concat(:q, '%') then 1
			              when lower(s.city) like concat(:q, '%') then 2
			              else 3 end, s.name
			""")
	List<Station> search(@Param("q") String query, Pageable pageable);
}
