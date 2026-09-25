package com.smarttransit.smart_transit.booking;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.smarttransit.smart_transit.booking.BookingDtos.BookingDto;
import com.smarttransit.smart_transit.booking.BookingDtos.BookingSummaryDto;
import com.smarttransit.smart_transit.booking.BookingDtos.PassengerSummary;
import com.smarttransit.smart_transit.booking.BookingDtos.PaymentSummary;
import com.smarttransit.smart_transit.booking.BookingDtos.SeatSummary;
import com.smarttransit.smart_transit.booking.BookingDtos.SegmentSummary;
import com.smarttransit.smart_transit.booking.BookingDtos.StationRef;
import com.smarttransit.smart_transit.journey.Journey;
import com.smarttransit.smart_transit.journey.JourneySegment;
import com.smarttransit.smart_transit.network.Station;
import com.smarttransit.smart_transit.payment.Payment;
import com.smarttransit.smart_transit.payment.PaymentProvider;
import com.smarttransit.smart_transit.payment.PaymentRepository;
import com.smarttransit.smart_transit.schedule.Trip;
import com.smarttransit.smart_transit.ticket.TicketRepository;

/** Maps bookings to DTOs. Must be called inside a transaction because it walks lazy associations. */
@Component
public class BookingAssembler {

	private static final Set<BookingStatus> CANCELLABLE = Set.of(BookingStatus.HELD, BookingStatus.CONFIRMED);

	private final SeatReservationRepository reservations;
	private final PaymentRepository payments;
	private final TicketRepository tickets;
	private final PaymentProvider provider;
	private final Clock clock;

	public BookingAssembler(SeatReservationRepository reservations, PaymentRepository payments,
			TicketRepository tickets, PaymentProvider provider, Clock clock) {
		this.reservations = reservations;
		this.payments = payments;
		this.tickets = tickets;
		this.provider = provider;
		this.clock = clock;
	}

	public BookingSummaryDto summary(Booking booking) {
		Journey journey = booking.getJourney();
		boolean cancellable = CANCELLABLE.contains(booking.getStatus())
				&& journey.getDeparture().isAfter(LocalDateTime.now(clock));
		return new BookingSummaryDto(booking.getId(), booking.getReference(), booking.getStatus(),
				booking.getTotalAmount(), booking.getCurrency(), booking.getHoldExpiresAt(), booking.getCreatedAt(),
				ref(journey.getOrigin()), ref(journey.getDestination()), journey.getDeparture(),
				journey.getArrival(), journey.getTransfers(),
				journey.getSegments().stream().map(JourneySegment::getMode).toList(),
				booking.getPassengers().size(), cancellable);
	}

	public BookingDto detail(Booking booking) {
		List<SeatReservation> seats = reservations.findByBooking(booking.getId());
		List<SegmentSummary> segments = booking.getJourney().getSegments().stream().map(this::segment).toList();

		List<PassengerSummary> passengers = new ArrayList<>();
		for (BookingPassenger p : booking.getPassengers()) {
			List<SeatSummary> passengerSeats = seats.stream()
					.filter(r -> r.getPassenger() != null && r.getPassenger().getId().equals(p.getId()))
					.sorted(Comparator.comparingInt(r -> r.getSegment().getSequence()))
					.map(r -> new SeatSummary(r.getSegment().getSequence(), r.getSeat().getLabel(),
							r.getSeat().getCoach().getCode(), r.getSeat().getCoach().getCoachClass().label(),
							r.getFare()))
					.toList();
			passengers.add(new PassengerSummary(p.getId(), p.getFullName(), p.getAge(), p.getGender(), p.getMobile(),
					passengerSeats));
		}
		PaymentSummary payment = payments.findFirstByBookingIdOrderByIdDesc(booking.getId()).map(this::payment)
				.orElse(null);
		String ticketNumber = tickets.findByBookingId(booking.getId()).map(t -> t.getTicketNumber()).orElse(null);
		return new BookingDto(summary(booking), segments, passengers, payment, ticketNumber,
				booking.getConfirmedAt(), booking.getCancelledAt(), booking.getCancellationReason(),
				booking.getRefundAmount());
	}

	private SegmentSummary segment(JourneySegment s) {
		Trip trip = s.getTrip();
		return new SegmentSummary(s.getSequence(), trip.getId(), s.getMode(), trip.getRoute().getName(),
				trip.getVehicle().getName(), trip.getVehicle().getNumber(), trip.getRoute().getOperator().getName(),
				ref(s.getBoardStation()), ref(s.getAlightStation()), s.getDeparture(), s.getArrival(), s.getFare(),
				trip.getStatus(), trip.getDelayMinutes());
	}

	private PaymentSummary payment(Payment p) {
		return new PaymentSummary(p.getId(), p.getStatus(), p.getMethod(), p.getProvider(),
				provider.name().equals(p.getProvider()) && provider.isSimulated(), p.getAmount(),
				p.getProviderReference(), p.getRefundStatus(), p.getRefundAmount(), p.getFailureReason(),
				p.getCreatedAt());
	}

	private static StationRef ref(Station s) {
		return new StationRef(s.getId(), s.getCode(), s.getName(), s.getCity());
	}
}
