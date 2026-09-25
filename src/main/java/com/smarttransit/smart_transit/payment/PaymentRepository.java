package com.smarttransit.smart_transit.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findFirstByBookingIdOrderByIdDesc(Long bookingId);

	Optional<Payment> findFirstByBookingIdAndStatus(Long bookingId, PaymentStatus status);

	@Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = com.smarttransit.smart_transit.payment.PaymentStatus.SUCCEEDED")
	BigDecimal totalCollected();

	@Query("""
			select coalesce(sum(p.refundAmount), 0) from Payment p
			where p.refundStatus = com.smarttransit.smart_transit.payment.RefundStatus.REFUNDED
			""")
	BigDecimal totalRefunded();

	@Query("""
			select p from Payment p join fetch p.booking
			where p.status = com.smarttransit.smart_transit.payment.PaymentStatus.SUCCEEDED and p.createdAt >= :since
			""")
	java.util.List<Payment> findSucceededSince(@Param("since") Instant since);
}
