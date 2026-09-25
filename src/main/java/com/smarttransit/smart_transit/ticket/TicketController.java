package com.smarttransit.smart_transit.ticket;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smarttransit.smart_transit.security.CurrentUser;

@RestController
@RequestMapping("/api/bookings/{bookingId}/ticket")
public class TicketController {

	private final TicketService tickets;
	private final TicketPdfRenderer pdfRenderer;

	public TicketController(TicketService tickets, TicketPdfRenderer pdfRenderer) {
		this.tickets = tickets;
		this.pdfRenderer = pdfRenderer;
	}

	@GetMapping
	TicketDto view(CurrentUser user, @PathVariable Long bookingId) {
		return tickets.forBooking(user, bookingId);
	}

	@GetMapping(value = "/download", produces = "application/pdf")
	ResponseEntity<byte[]> download(CurrentUser user, @PathVariable Long bookingId) {
		TicketDto ticket = tickets.forBooking(user, bookingId);
		byte[] pdf = pdfRenderer.render(ticket);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + ticket.ticketNumber() + ".pdf\"")
				.body(pdf);
	}
}
