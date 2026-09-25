package com.smarttransit.smart_transit.timetable;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

	/** Every imported service that runs directly from the origin to the destination, earliest first. */
	@Query("""
			select e from TimetableEntry e
			join fetch e.origin join fetch e.destination left join fetch e.schedule
			where e.origin.id = :originId and e.destination.id = :destinationId
			order by e.departureTime, e.serviceNumber
			""")
	List<TimetableEntry> findForPair(@Param("originId") long originId, @Param("destinationId") long destinationId);
}
