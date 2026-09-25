package com.smarttransit.smart_transit.ticket;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	Optional<Ticket> findByBookingId(Long bookingId);

	Optional<Ticket> findByTicketNumber(String ticketNumber);
}
