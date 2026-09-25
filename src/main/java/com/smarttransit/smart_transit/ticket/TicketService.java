package com.smarttransit.smart_transit.ticket;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.booking.Booking;
import com.smarttransit.smart_transit.booking.BookingPassenger;
import com.smarttransit.smart_transit.booking.BookingRepository;
import com.smarttransit.smart_transit.booking.SeatReservation;
import com.smarttransit.smart_transit.booking.SeatReservationRepository;
import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.journey.JourneySegment;
import com.smarttransit.smart_transit.security.CurrentUser;
import com.smarttransit.smart_transit.security.HmacSigner;

@Service
public class TicketService {

	private final TicketRepository tickets;
	private final BookingRepository bookings;
	private final SeatReservationRepository reservations;
	private final QrCodeService qrCodes;
	private final HmacSigner signer;
	private final Clock clock;

	public TicketService(TicketRepository tickets, BookingRepository bookings, SeatReservationRepository reservations,
			QrCodeService qrCodes, HmacSigner signer, Clock clock) {
		this.tickets = tickets;
		this.bookings = bookings;
		this.reservations = reservations;
		this.qrCodes = qrCodes;
		this.signer = signer;
		this.clock = clock;
	}

	/** Issues the ticket for a just-confirmed booking. The QR payload is signed so {@link #verify} needs no lookup. */
	@Transactional
	public Ticket issue(Booking booking) {
		String ticketNumber = "TCK" + booking.getReference().replace("KV-", "");
		String data = ticketNumber + "|" + booking.getReference();
		String qrPayload = data + "|" + signer.sign(data);
		return tickets.save(new Ticket(booking, ticketNumber, qrPayload, Instant.now(clock)));
	}

	@Transactional
	public void cancelFor(Long bookingId) {
		tickets.findByBookingId(bookingId).ifPresent(Ticket::cancel);
	}

	/** True if the QR content is authentic and matches an active ticket for its booking. */
	@Transactional(readOnly = true)
	public boolean verify(String qrPayload) {
		int lastPipe = qrPayload == null ? -1 : qrPayload.lastIndexOf('|');
		if (lastPipe < 0) {
			return false;
		}
		String data = qrPayload.substring(0, lastPipe);
		String signature = qrPayload.substring(lastPipe + 1);
		String[] parts = data.split("\\|");
		return parts.length == 2 && signer.verify(data, signature)
				&& tickets.findByTicketNumber(parts[0])
						.filter(t -> t.getStatus() == TicketStatus.ACTIVE && t.getBooking().getReference().equals(parts[1]))
						.isPresent();
	}

	@Transactional(readOnly = true)
	public TicketDto forBooking(CurrentUser user, Long bookingId) {
		Booking booking = bookings.findDetailedById(bookingId)
				.filter(b -> user.isAdmin() || b.getUser().getId().equals(user.id()))
				.orElseThrow(() -> ApiException.notFound("Booking", bookingId));
		Ticket ticket = tickets.findByBookingId(bookingId).orElseThrow(() -> ApiException.notFound("Ticket", bookingId));
		return toDto(booking, ticket);
	}

	private TicketDto toDto(Booking booking, Ticket ticket) {
		List<SeatReservation> seats = reservations.findByBooking(booking.getId());
		List<TicketDto.PassengerLine> passengers = booking.getPassengers().stream().map(p -> {
			List<TicketDto.SeatLine> lines = seats.stream()
					.filter(r -> r.getPassenger() != null && r.getPassenger().getId().equals(p.getId()))
					.sorted((a, b) -> Integer.compare(a.getSegment().getSequence(), b.getSegment().getSequence()))
					.map(r -> new TicketDto.SeatLine(r.getSegment().getSequence(), r.getSeat().getLabel(),
							r.getSeat().getCoach().getCode(), r.getSeat().getCoach().getCoachClass().label()))
					.toList();
			return new TicketDto.PassengerLine(p.getFullName(), p.getAge(), p.getGender().name(), lines);
		}).toList();

		List<com.smarttransit.smart_transit.booking.BookingDtos.SegmentSummary> segments = booking.getJourney()
				.getSegments().stream().map(this::segmentSummary).toList();
		return new TicketDto(ticket.getTicketNumber(), booking.getReference(), ticket.getStatus(),
				ticket.getIssuedAt(), stationRef(booking.getJourney().getOrigin()),
				stationRef(booking.getJourney().getDestination()), booking.getUser().getFullName(),
				booking.getTotalAmount(), booking.getCurrency(), segments, passengers, ticket.getQrPayload(),
				qrCodes.svgDataUri(ticket.getQrPayload()));
	}

	private com.smarttransit.smart_transit.booking.BookingDtos.SegmentSummary segmentSummary(JourneySegment s) {
		var trip = s.getTrip();
		return new com.smarttransit.smart_transit.booking.BookingDtos.SegmentSummary(s.getSequence(), trip.getId(),
				s.getMode(), trip.getRoute().getName(), trip.getVehicle().getName(), trip.getVehicle().getNumber(),
				trip.getRoute().getOperator().getName(), stationRef(s.getBoardStation()),
				stationRef(s.getAlightStation()), s.getDeparture(), s.getArrival(), s.getFare(), trip.getStatus(),
				trip.getDelayMinutes());
	}

	private static com.smarttransit.smart_transit.booking.BookingDtos.StationRef stationRef(
			com.smarttransit.smart_transit.network.Station station) {
		return new com.smarttransit.smart_transit.booking.BookingDtos.StationRef(station.getId(), station.getCode(),
				station.getName(), station.getCity());
	}
}
