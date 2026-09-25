package com.smarttransit.smart_transit.booking;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

	String ACTIVE = """
			(r.status = com.smarttransit.smart_transit.booking.ReservationStatus.BOOKED
			  or (r.status = com.smarttransit.smart_transit.booking.ReservationStatus.HELD and r.expiresAt > :now))
			""";

	@Query("""
			select new com.smarttransit.smart_transit.booking.ActiveSeat(r.trip.id, r.seat.id, r.seat.coach.id,
			  r.boardSequence, r.alightSequence)
			from SeatReservation r where r.trip.id in :tripIds and\s""" + ACTIVE)
	List<ActiveSeat> findActiveForTrips(@Param("tripIds") Collection<Long> tripIds, @Param("now") Instant now);

	@Query("""
			select r from SeatReservation r join fetch r.seat left join fetch r.passenger
			where r.trip.id = :tripId and r.boardSequence < :alight and :board < r.alightSequence and\s""" + ACTIVE)
	List<SeatReservation> findActiveOverlapping(@Param("tripId") Long tripId, @Param("board") int board,
			@Param("alight") int alight, @Param("now") Instant now);

	@Query("""
			select r from SeatReservation r
			where r.trip.id = :tripId and r.seat.id = :seatId and r.boardSequence < :alight
			  and :board < r.alightSequence and\s""" + ACTIVE)
	List<SeatReservation> findConflicts(@Param("tripId") Long tripId, @Param("seatId") Long seatId,
			@Param("board") int board, @Param("alight") int alight, @Param("now") Instant now);

	@Query("""
			select r from SeatReservation r join fetch r.seat s join fetch s.coach c join fetch r.trip t
			where r.user.id = :userId and r.booking is null
			  and r.status = com.smarttransit.smart_transit.booking.ReservationStatus.HELD and r.expiresAt > :now
			order by r.createdAt
			""")
	List<SeatReservation> findLooseHolds(@Param("userId") Long userId, @Param("now") Instant now);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select r from SeatReservation r join fetch r.seat s join fetch s.coach c
			where r.id in :ids and r.user.id = :userId
			""")
	List<SeatReservation> findOwned(@Param("ids") Collection<Long> ids, @Param("userId") Long userId);

	@Query("select r from SeatReservation r join fetch r.seat s join fetch s.coach c where r.booking.id = :bookingId")
	List<SeatReservation> findByBooking(@Param("bookingId") Long bookingId);

	@Query("""
			select count(r) from SeatReservation r
			where r.user.id = :userId and r.trip.id = :tripId and r.booking is null
			  and r.status = com.smarttransit.smart_transit.booking.ReservationStatus.HELD and r.expiresAt > :now
			""")
	long countLooseHolds(@Param("userId") Long userId, @Param("tripId") Long tripId, @Param("now") Instant now);

	@Modifying
	@Query("""
			update SeatReservation r set r.status = com.smarttransit.smart_transit.booking.ReservationStatus.EXPIRED
			where r.status = com.smarttransit.smart_transit.booking.ReservationStatus.HELD
			  and r.booking is null and r.expiresAt <= :now
			""")
	int expireLooseHolds(@Param("now") Instant now);

	Optional<SeatReservation> findByIdAndUserId(Long id, Long userId);

	@Query("select distinct r.user from SeatReservation r where r.trip.id = :tripId and r.status = com.smarttransit.smart_transit.booking.ReservationStatus.BOOKED")
	List<com.smarttransit.smart_transit.user.User> findTravellersOnTrip(@Param("tripId") Long tripId);
}
