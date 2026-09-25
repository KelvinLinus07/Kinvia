package com.smarttransit.smart_transit.booking;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarttransit.smart_transit.booking.BookingDtos.BookingDto;
import com.smarttransit.smart_transit.booking.BookingDtos.BookingSummaryDto;
import com.smarttransit.smart_transit.booking.BookingDtos.CancellationPreview;
import com.smarttransit.smart_transit.booking.BookingDtos.CreateBookingRequest;
import com.smarttransit.smart_transit.booking.BookingDtos.PassengerInput;
import com.smarttransit.smart_transit.booking.BookingDtos.SeatAssignment;
import com.smarttransit.smart_transit.common.ApiException;
import com.smarttransit.smart_transit.common.PageResponse;
import com.smarttransit.smart_transit.config.KinviaProperties;
import com.smarttransit.smart_transit.journey.Journey;
import com.smarttransit.smart_transit.journey.JourneyKey;
import com.smarttransit.smart_transit.journey.JourneySegment;
import com.smarttransit.smart_transit.journey.Leg;
import com.smarttransit.smart_transit.journey.LegCatalog;
import com.smarttransit.smart_transit.network.StationRepository;
import com.smarttransit.smart_transit.notification.NotificationService;
import com.smarttransit.smart_transit.notification.NotificationType;
import com.smarttransit.smart_transit.payment.PaymentService;
import com.smarttransit.smart_transit.payment.RefundStatus;
import com.smarttransit.smart_transit.schedule.TripRepository;
import com.smarttransit.smart_transit.security.CurrentUser;
import com.smarttransit.smart_transit.ticket.TicketService;
import com.smarttransit.smart_transit.user.UserRepository;

@Service
public class BookingService {

	private final BookingRepository bookings;
	private final SeatReservationRepository reservations;
	private final LegCatalog legCatalog;
	private final StationRepository stations;
	private final TripRepository trips;
	private final UserRepository users;
	private final BookingAssembler assembler;
	private final CancellationPolicy cancellationPolicy;
	private final PaymentService payments;
	private final TicketService tickets;
	private final NotificationService notifications;
	private final KinviaProperties properties;
	private final Clock clock;

	public BookingService(BookingRepository bookings, SeatReservationRepository reservations, LegCatalog legCatalog,
			StationRepository stations, TripRepository trips, UserRepository users, BookingAssembler assembler,
			CancellationPolicy cancellationPolicy, PaymentService payments, TicketService tickets,
			NotificationService notifications, KinviaProperties properties, Clock clock) {
		this.bookings = bookings;
		this.reservations = reservations;
		this.legCatalog = legCatalog;
		this.stations = stations;
		this.trips = trips;
		this.users = users;
		this.assembler = assembler;
		this.cancellationPolicy = cancellationPolicy;
		this.payments = payments;
		this.tickets = tickets;
		this.notifications = notifications;
		this.properties = properties;
		this.clock = clock;
	}

	/** Turns a journey plus the caller's live seat holds into a booking that awaits payment. */
	@Transactional
	public BookingDto create(CurrentUser user, CreateBookingRequest request) {
		List<PassengerInput> travellers = request.passengers();
		if (travellers.size() > properties.booking().maxPassengers()) {
			throw ApiException.badRequest("A booking can include at most " + properties.booking().maxPassengers() + " passengers");
		}
		List<Leg> legs = legCatalog.resolve(JourneyKey.parse(request.journeyKey()));
		String reference = uniqueReference();
		Map<Long, SeatReservation> holds = loadHolds(user, request, legs, travellers.size());

		Instant now = Instant.now(clock);
		Instant expiresAt = now.plus(properties.booking().holdDuration());
		Leg first = legs.getFirst();
		Leg last = legs.getLast();
		Journey journey = new Journey(users.getReferenceById(user.id()), stations.getReferenceById(first.board().stationId()),
				stations.getReferenceById(last.alight().stationId()), first.departure(), last.arrival());
		List<JourneySegment> segments = new ArrayList<>();
		for (int i = 0; i < legs.size(); i++) {
			Leg leg = legs.get(i);
			segments.add(journey.addSegment(new JourneySegment(journey, i, trips.getReferenceById(leg.trip().tripId()),
					stations.getReferenceById(leg.board().stationId()),
					stations.getReferenceById(leg.alight().stationId()), leg)));
		}

		BigDecimal[] segmentFares = new BigDecimal[legs.size()];
		java.util.Arrays.fill(segmentFares, BigDecimal.ZERO);
		BigDecimal total = BigDecimal.ZERO;
		Booking booking = new Booking(reference, users.getReferenceById(user.id()), journey, BigDecimal.ZERO, expiresAt);
		List<BookingPassenger> passengers = travellers.stream()
				.map(p -> booking.addPassenger(p.fullName(), p.age(), p.gender(), p.mobile())).toList();
		for (SeatAssignment assignment : request.seats()) {
			SeatReservation hold = holds.get(assignment.holdId());
			hold.attachTo(booking, passengers.get(assignment.passengerIndex()), segments.get(assignment.segmentIndex()),
					expiresAt);
			segmentFares[assignment.segmentIndex()] = segmentFares[assignment.segmentIndex()].add(hold.getFare());
			total = total.add(hold.getFare());
		}
		for (int i = 0; i < segments.size(); i++) {
			segments.get(i).setFare(segmentFares[i]);
		}
		journey.setTotalFare(total);
		booking.setTotalAmount(total);
		bookings.save(booking);
		return assembler.detail(booking);
	}

	private Map<Long, SeatReservation> loadHolds(CurrentUser user, CreateBookingRequest request, List<Leg> legs,
			int passengerCount) {
		List<SeatAssignment> assignments = request.seats();
		if (assignments.size() != legs.size() * passengerCount) {
			throw ApiException.badRequest("Choose a seat for every passenger on every part of the journey");
		}
		Set<String> slots = new HashSet<>();
		Set<Long> holdIds = new HashSet<>();
		for (SeatAssignment a : assignments) {
			if (a.segmentIndex() >= legs.size() || a.passengerIndex() >= passengerCount
					|| !slots.add(a.segmentIndex() + ":" + a.passengerIndex()) || !holdIds.add(a.holdId())) {
				throw ApiException.badRequest("Seat selection is inconsistent with the journey");
			}
		}
		Map<Long, SeatReservation> holds = new HashMap<>();
		reservations.findOwned(holdIds, user.id()).forEach(r -> holds.put(r.getId(), r));
		Instant now = Instant.now(clock);
		for (SeatAssignment a : assignments) {
			SeatReservation hold = holds.get(a.holdId());
			Leg leg = legs.get(a.segmentIndex());
			boolean valid = hold != null && hold.getBooking() == null && hold.isActiveAt(now)
					&& hold.getStatus() == ReservationStatus.HELD
					&& hold.getTrip().getId() == leg.trip().tripId()
					&& hold.getBoardSequence() == leg.boardIndex() && hold.getAlightSequence() == leg.alightIndex();
			if (!valid) {
				throw ApiException.conflict("A seat hold has expired or no longer matches this journey. Please choose seats again.");
			}
		}
		return holds;
	}

	private String uniqueReference() {
		String reference = BookingReferences.next();
		while (bookings.existsByReference(reference)) {
			reference = BookingReferences.next();
		}
		return reference;
	}

	@Transactional(readOnly = true)
	public BookingDto get(CurrentUser user, Long id) {
		return assembler.detail(ownedBooking(user, id));
	}

	@Transactional(readOnly = true)
	public PageResponse<BookingSummaryDto> list(CurrentUser user, BookingScope scope, int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
		LocalDateTime now = LocalDateTime.now(clock);
		var result = switch (scope) {
			case UPCOMING -> bookings.findUpcoming(user.id(), now, pageable);
			case PREVIOUS -> bookings.findPrevious(user.id(), now, pageable);
			case ALL -> bookings.findByUserIdOrderByCreatedAtDesc(user.id(), pageable);
		};
		return PageResponse.of(result, assembler::summary);
	}

	@Transactional(readOnly = true)
	public PageResponse<BookingSummaryDto> adminList(BookingStatus status, int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
		return PageResponse.of(bookings.findAllByStatus(status, pageable), assembler::summary);
	}

	@Transactional(readOnly = true)
	public PageResponse<BookingSummaryDto> operatorList(Long operatorId, BookingStatus status, int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
		return PageResponse.of(bookings.findForOperator(operatorId, status, pageable), assembler::summary);
	}

	@Transactional(readOnly = true)
	public CancellationPreview previewCancellation(CurrentUser user, Long id) {
		return previewCancellation(ownedBooking(user, id));
	}

	private CancellationPreview confirmedPreview(Booking booking) {
		Duration untilDeparture = Duration.between(LocalDateTime.now(clock), booking.getJourney().getDeparture());
		if (untilDeparture.isNegative() || untilDeparture.isZero()) {
			return new CancellationPreview(false, 0, BigDecimal.ZERO,
					"This journey has already departed and cannot be cancelled.");
		}
		CancellationPolicy.Refund refund = cancellationPolicy.refundFor(booking.getTotalAmount(), untilDeparture);
		return new CancellationPreview(true, refund.percent(), refund.amount(), refund.explanation());
	}

	@Transactional
	public BookingDto cancel(CurrentUser user, Long id) {
		Booking booking = bookings.lockById(id)
				.filter(b -> user.isAdmin() || b.getUser().getId().equals(user.id()))
				.orElseThrow(() -> ApiException.notFound("Booking", id));
		CancellationPreview preview = previewCancellation(booking);
		if (!preview.cancellable()) {
			throw ApiException.conflict(preview.message());
		}
		applyCancellation(booking, "Cancelled by traveller", preview.refundAmount(), preview.message());
		return assembler.detail(booking);
	}

	/** Cancels a confirmed booking because its service was cancelled, refunding in full. */
	@Transactional
	public void cancelForServiceChange(Long bookingId, String reason) {
		bookings.lockById(bookingId).filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
				.ifPresent(b -> applyCancellation(b, reason, cancellationPolicy.fullRefund(b.getTotalAmount()).amount(),
						reason + " You have been refunded in full."));
	}

	private CancellationPreview previewCancellation(Booking booking) {
		return switch (booking.getStatus()) {
			case HELD -> new CancellationPreview(true, 0, BigDecimal.ZERO,
					"Your held seats will be released. You have not been charged.");
			case CONFIRMED -> confirmedPreview(booking);
			default -> new CancellationPreview(false, 0, BigDecimal.ZERO,
					"A booking with status " + booking.getStatus() + " cannot be cancelled.");
		};
	}

	private void applyCancellation(Booking booking, String reason, BigDecimal refundAmount, String detail) {
		boolean wasPaid = booking.getStatus() == BookingStatus.CONFIRMED;
		booking.cancel(Instant.now(clock), reason, refundAmount);
		reservations.findByBooking(booking.getId()).forEach(SeatReservation::release);
		if (wasPaid) {
			tickets.cancelFor(booking.getId());
			RefundStatus outcome = payments.refundFor(booking, refundAmount);
			if (outcome == RefundStatus.FAILED) {
				detail += " The refund could not be processed automatically; our team will follow up.";
			}
		}
		notifications.send(booking.getUser(), NotificationType.BOOKING_CANCELLED, "Booking cancelled",
				"Booking " + booking.getReference() + " was cancelled. " + detail, booking);
	}

	private Booking ownedBooking(CurrentUser user, Long id) {
		return bookings.findDetailedById(id).filter(b -> user.isAdmin() || b.getUser().getId().equals(user.id()))
				.orElseThrow(() -> ApiException.notFound("Booking", id));
	}
}
