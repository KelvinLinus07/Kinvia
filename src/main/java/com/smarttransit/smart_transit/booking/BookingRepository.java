package com.smarttransit.smart_transit.booking;

import java.time.Instant;
import java.time.LocalDateTime;
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

import jakarta.persistence.LockModeType;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	String LIVE_STATUSES = "(com.smarttransit.smart_transit.booking.BookingStatus.HELD, com.smarttransit.smart_transit.booking.BookingStatus.PENDING, com.smarttransit.smart_transit.booking.BookingStatus.CONFIRMED)";

	boolean existsByReference(String reference);

	@EntityGraph(attributePaths = { "user", "journey", "journey.origin", "journey.destination" })
	@Query("select b from Booking b where b.id = :id")
	Optional<Booking> findDetailedById(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from Booking b join fetch b.user join fetch b.journey j join fetch j.origin join fetch j.destination where b.id = :id")
	Optional<Booking> lockById(@Param("id") Long id);

	@EntityGraph(attributePaths = { "journey", "journey.origin", "journey.destination" })
	@Query(value = "select b from Booking b where b.user.id = :userId and b.status in " + LIVE_STATUSES
			+ " and b.journey.arrival >= :now order by b.journey.departure",
			countQuery = "select count(b) from Booking b where b.user.id = :userId and b.status in " + LIVE_STATUSES
					+ " and b.journey.arrival >= :now")
	Page<Booking> findUpcoming(@Param("userId") Long userId, @Param("now") LocalDateTime now, Pageable pageable);

	@EntityGraph(attributePaths = { "journey", "journey.origin", "journey.destination" })
	@Query(value = "select b from Booking b where b.user.id = :userId and not (b.status in " + LIVE_STATUSES
			+ " and b.journey.arrival >= :now) order by b.journey.departure desc",
			countQuery = "select count(b) from Booking b where b.user.id = :userId and not (b.status in "
					+ LIVE_STATUSES + " and b.journey.arrival >= :now)")
	Page<Booking> findPrevious(@Param("userId") Long userId, @Param("now") LocalDateTime now, Pageable pageable);

	@EntityGraph(attributePaths = { "journey", "journey.origin", "journey.destination" })
	Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	@EntityGraph(attributePaths = { "user", "journey", "journey.origin", "journey.destination" })
	@Query(value = "select b from Booking b where (:status is null or b.status = :status) order by b.createdAt desc",
			countQuery = "select count(b) from Booking b where (:status is null or b.status = :status)")
	Page<Booking> findAllByStatus(@Param("status") BookingStatus status, Pageable pageable);

	/** Bookings touching any trip of the given operator (distinct via segments). */
	@EntityGraph(attributePaths = { "user", "journey", "journey.origin", "journey.destination" })
	@Query(value = """
			select b from Booking b where (:status is null or b.status = :status) and exists (
			  select 1 from JourneySegment s where s.journey = b.journey and s.trip.route.operator.id = :operatorId)
			order by b.createdAt desc
			""", countQuery = """
			select count(b) from Booking b where (:status is null or b.status = :status) and exists (
			  select 1 from JourneySegment s where s.journey = b.journey and s.trip.route.operator.id = :operatorId)
			""")
	Page<Booking> findForOperator(@Param("operatorId") Long operatorId, @Param("status") BookingStatus status,
			Pageable pageable);

	@Query("""
			select b from Booking b join fetch b.user
			where (b.status = com.smarttransit.smart_transit.booking.BookingStatus.HELD and b.holdExpiresAt <= :now)
			   or (b.status = com.smarttransit.smart_transit.booking.BookingStatus.PENDING and b.holdExpiresAt <= :pendingCutoff)
			""")
	List<Booking> findExpirable(@Param("now") Instant now, @Param("pendingCutoff") Instant pendingCutoff);

	@Query("""
			select b from Booking b join fetch b.user join fetch b.journey j join fetch j.origin join fetch j.destination
			where b.status = com.smarttransit.smart_transit.booking.BookingStatus.CONFIRMED and j.arrival < :now
			""")
	List<Booking> findFinished(@Param("now") LocalDateTime now);

	@Query("""
			select b from Booking b join fetch b.user join fetch b.journey j join fetch j.origin join fetch j.destination
			where b.status = com.smarttransit.smart_transit.booking.BookingStatus.CONFIRMED and b.reminderSent = false
			  and j.departure > :now and j.departure <= :until
			""")
	List<Booking> findNeedingReminder(@Param("now") LocalDateTime now, @Param("until") LocalDateTime until);

	/** Confirmed bookings with at least one segment on the trip. */
	@Query("""
			select distinct b from Booking b join fetch b.user join fetch b.journey j join fetch j.origin join fetch j.destination
			where b.status = com.smarttransit.smart_transit.booking.BookingStatus.CONFIRMED and exists (
			  select 1 from JourneySegment s where s.journey = j and s.trip.id = :tripId)
			""")
	List<Booking> findConfirmedOnTrip(@Param("tripId") Long tripId);

	long countByStatus(BookingStatus status);

	@Query("select b.createdAt, b.totalAmount, b.status from Booking b where b.createdAt >= :since")
	List<Object[]> findTrendSince(@Param("since") Instant since);
}
