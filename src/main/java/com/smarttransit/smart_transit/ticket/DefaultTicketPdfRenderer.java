package com.smarttransit.smart_transit.ticket;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.smarttransit.smart_transit.booking.BookingDtos.SegmentSummary;

@Component
public class DefaultTicketPdfRenderer implements TicketPdfRenderer {

	private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
	private final QrCodeService qrCodes;

	public DefaultTicketPdfRenderer(QrCodeService qrCodes) {
		this.qrCodes = qrCodes;
	}

	@Override
	public byte[] render(TicketDto ticket) {
		MinimalPdfWriter pdf = new MinimalPdfWriter();
		double margin = 48;
		double y = pdf.pageHeight() - 60;

		pdf.boldText(margin, y, 22, "KINVIA");
		pdf.text(margin, y - 18, 10, "Electronic ticket - not valid without a matching QR scan");
		y -= 50;
		pdf.line(margin, y, pdf.pageWidth() - margin, y, 1);
		y -= 28;

		pdf.boldText(margin, y, 13, ticket.origin().name() + "  -->  " + ticket.destination().name());
		pdf.text(pdf.pageWidth() - margin - 160, y, 10, "Ticket " + ticket.ticketNumber());
		y -= 16;
		pdf.text(margin, y, 10, "Booking reference " + ticket.bookingReference() + "   Status: " + ticket.status());
		y -= 28;

		for (SegmentSummary segment : ticket.segments()) {
			pdf.boldText(margin, y, 11,
					"Segment " + (segment.order() + 1) + ": " + segment.mode() + " - " + segment.routeName());
			y -= 14;
			pdf.text(margin, y, 9.5,
					segment.vehicleName() + " (" + segment.vehicleNumber() + ") operated by " + segment.operatorName());
			y -= 13;
			pdf.text(margin, y, 9.5, segment.board().name() + " " + segment.departure().format(WHEN) + "  ->  "
					+ segment.alight().name() + " " + segment.arrival().format(WHEN));
			y -= 13;
			pdf.text(margin, y, 9.5, "Fare for this segment: " + ticket.currency() + " " + segment.fare());
			y -= 22;
		}

		pdf.line(margin, y, pdf.pageWidth() - margin, y, 0.5);
		y -= 20;
		pdf.boldText(margin, y, 11, "Passengers");
		y -= 16;
		for (TicketDto.PassengerLine passenger : ticket.passengers()) {
			StringBuilder seats = new StringBuilder();
			passenger.seats().forEach(s -> seats.append(s.classLabel()).append(' ').append(s.coachCode()).append('-')
					.append(s.seatLabel()).append("; "));
			pdf.text(margin, y, 9.5,
					passenger.fullName() + " (" + passenger.age() + ", " + passenger.gender() + ") - " + seats);
			y -= 14;
		}

		y -= 10;
		pdf.boldText(margin, y, 11, "Total paid: " + ticket.currency() + " " + ticket.totalAmount());
		pdf.qr(qrCodes.matrix(ticket.qrPayload()), pdf.pageWidth() - margin - 110, y - 110, 110);
		return pdf.build();
	}
}
