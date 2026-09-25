package com.smarttransit.smart_transit.ticket;

import com.smarttransit.smart_transit.ticket.TicketDto;

/** Renders a ticket as a downloadable document. Swap the implementation for a richer PDF library if needed. */
public interface TicketPdfRenderer {

	byte[] render(TicketDto ticket);
}
