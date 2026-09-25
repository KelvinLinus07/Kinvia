import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api, getToken } from '../api/client';
import RouteLine from '../components/RouteLine';
import { formatDateTime, formatMoney } from '../utils/format';

export default function TicketPage() {
  const { bookingId } = useParams();
  const [ticket, setTicket] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.get(`/api/bookings/${bookingId}/ticket`).then(setTicket).catch((err) => setError(err.message));
  }, [bookingId]);

  if (error) return <div className="container" style={{ padding: 60 }}><p className="banner banner-error">{error}</p></div>;
  if (!ticket) return <div className="container" style={{ padding: 60 }}><div className="skeleton" style={{ height: 260 }} /></div>;

  const segmentsAsRoute = ticket.segments.map((s) => ({ mode: s.mode, board: s.board, alight: s.alight }));

  return (
    <div className="container" style={{ padding: '40px 0 64px', maxWidth: 820 }}>
      <div className="ticket">
        <div style={{ padding: 32 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <div className="brand" style={{ fontSize: '1.2rem' }}>Kinvia</div>
              <p className="journey-sub">Ticket {ticket.ticketNumber}</p>
            </div>
            <span className={`pill ${ticket.status === 'ACTIVE' ? 'pill-teal' : 'pill-red'}`}>{ticket.status}</span>
          </div>

          <h2 style={{ marginTop: 20 }}>{ticket.origin.name} &rarr; {ticket.destination.name}</h2>
          <RouteLine segments={segmentsAsRoute} />

          {ticket.segments.map((segment, i) => (
            <div key={i} className="segment-row">
              <span className="pill pill-ink">{segment.mode}</span>
              <div style={{ flex: 1 }}>
                <strong>{segment.routeName}</strong> &middot; {segment.vehicleName} ({segment.vehicleNumber}) &middot; {segment.operatorName}
                <div className="journey-sub">
                  {segment.board.name} {formatDateTime(segment.departure)} &rarr; {segment.alight.name} {formatDateTime(segment.arrival)}
                </div>
              </div>
            </div>
          ))}

          <h3 style={{ marginTop: 24, fontSize: '0.95rem' }}>Passengers</h3>
          {ticket.passengers.map((p, i) => (
            <p key={i} style={{ margin: '4px 0', fontSize: '0.9rem' }}>
              {p.fullName} ({p.age}, {p.gender.toLowerCase()}) &mdash;{' '}
              {p.seats.map((s) => `${s.classLabel} ${s.coachCode}-${s.seatLabel}`).join(', ')}
            </p>
          ))}

          <p style={{ marginTop: 20, fontWeight: 700 }}>Total paid: {formatMoney(ticket.totalAmount, ticket.currency)}</p>
        </div>
        <div className="ticket-qr">
          <img src={ticket.qrSvgDataUri} alt="Ticket QR code" width={140} height={140} />
          <span style={{ fontSize: '0.75rem', textAlign: 'center', opacity: 0.8 }}>Scan to verify at boarding</span>
          <a
            className="btn btn-primary btn-sm"
            href={`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/bookings/${bookingId}/ticket/download`}
            onClick={(e) => downloadWithAuth(e, bookingId)}
          >
            Download PDF
          </a>
        </div>
      </div>
    </div>
  );
}

async function downloadWithAuth(event, bookingId) {
  event.preventDefault();
  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
  const response = await fetch(`${base}/api/bookings/${bookingId}/ticket/download`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `kinvia-ticket-${bookingId}.pdf`;
  link.click();
  URL.revokeObjectURL(url);
}
