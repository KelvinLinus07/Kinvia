import { useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import BookingStepper from '../components/BookingStepper';
import ClassSelect from '../components/ClassSelect';
import RouteLine from '../components/RouteLine';
import SeatMap from '../components/SeatMap';
import { shortClassLabel } from '../utils/classLabels';
import { formatDateTime, formatDuration, formatMoney } from '../utils/format';

export default function BookingWizardPage() {
  const { journeyKey } = useParams();
  const [searchParams] = useSearchParams();
  const passengerCount = Number(searchParams.get('passengers') || 1);
  const navigate = useNavigate();
  const { user } = useAuth();

  const [step, setStep] = useState(0);
  const [journey, setJourney] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  // Keyed by segment index -> the ClassOfferDto the traveller picked for that leg (coachClass, label, fare,
  // availableSeats). This is what carries the chosen class through seat selection, the booking request and
  // ultimately the fare that gets charged.
  const [selectedClasses, setSelectedClasses] = useState({});
  const [seatMaps, setSeatMaps] = useState({});
  const [holdsBySegment, setHoldsBySegment] = useState({});
  const [passengers, setPassengers] = useState([]);
  const [savedPassengers, setSavedPassengers] = useState([]);
  const [booking, setBooking] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('UPI');
  const [instrument, setInstrument] = useState('');
  const [working, setWorking] = useState(false);

  useEffect(() => {
    api
      .get(`/api/journeys/${encodeURIComponent(journeyKey)}?passengers=${passengerCount}`)
      .then(setJourney)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
    setPassengers(Array.from({ length: passengerCount }, () => ({ fullName: '', age: '', gender: 'MALE', mobile: '' })));
    setSelectedClasses({});
    if (user) api.get('/api/users/me/passengers').then(setSavedPassengers).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [journeyKey, passengerCount]);

  if (!user) {
    return (
      <div className="container" style={{ padding: '60px 0', textAlign: 'center' }}>
        <h2>Sign in to book this journey</h2>
        <p>Your search results and seat selection are saved &mdash; just sign in or create an account to continue.</p>
        <button className="btn btn-primary" onClick={() => navigate('/login', { state: { from: window.location.pathname + window.location.search } })}>
          Sign in
        </button>
      </div>
    );
  }

  if (loading) return <div className="container" style={{ padding: 60 }}><div className="skeleton" style={{ height: 200 }} /></div>;
  if (error) return <div className="container" style={{ padding: 60 }}><p className="banner banner-error">{error}</p></div>;
  if (!journey) return null;

  const allSegmentsHaveClass = journey.segments.every((s) => selectedClasses[s.index]);
  const allSegmentsHaveSeats = journey.segments.every((s) => (holdsBySegment[s.index] || []).length === passengerCount);

  function chooseClass(segment, offer) {
    setSelectedClasses((prev) => ({ ...prev, [segment.index]: offer }));
  }

  async function loadSeatMap(segment) {
    const map = await api.get(`/api/trips/${segment.tripId}/seats?board=${segment.boardSequence}&alight=${segment.alightSequence}`);
    setSeatMaps((prev) => ({ ...prev, [segment.index]: map }));
  }

  async function continueToSeats() {
    setWorking(true);
    setError(null);
    try {
      await Promise.all(journey.segments.map(loadSeatMap));
      setStep(2);
    } catch (err) {
      setError(err.message);
    } finally {
      setWorking(false);
    }
  }

  /** Releases every seat hold taken so far, so switching class never leaves a hold on a seat in the old class. */
  async function releaseAllHolds() {
    const allHolds = Object.values(holdsBySegment).flat();
    await Promise.all(allHolds.map((h) => api.delete(`/api/holds/${h.id}`).catch(() => {})));
    setHoldsBySegment({});
  }

  async function backToClassSelection() {
    setWorking(true);
    try {
      await releaseAllHolds();
      setSeatMaps({});
      setStep(1);
    } finally {
      setWorking(false);
    }
  }

  async function toggleSeat(segment, seat) {
    const current = holdsBySegment[segment.index] || [];
    const already = current.find((h) => h.seatId === seat.id);
    if (already) {
      try {
        await api.delete(`/api/holds/${already.id}`);
      } catch { /* ignore */ }
      setHoldsBySegment((prev) => ({ ...prev, [segment.index]: current.filter((h) => h.seatId !== seat.id) }));
      return;
    }
    if (current.length >= passengerCount) return;
    setWorking(true);
    try {
      const hold = await api.post(
        `/api/trips/${segment.tripId}/seats/${seat.id}/hold?board=${segment.boardSequence}&alight=${segment.alightSequence}`,
      );
      setHoldsBySegment((prev) => ({ ...prev, [segment.index]: [...current, { ...hold, seatId: seat.id, seatLabel: seat.label }] }));
      loadSeatMap(segment);
    } catch (err) {
      setError(err.message);
    } finally {
      setWorking(false);
    }
  }

  async function submitBooking() {
    setWorking(true);
    setError(null);
    try {
      const seats = [];
      journey.segments.forEach((segment) => {
        (holdsBySegment[segment.index] || []).forEach((hold, passengerIndex) => {
          seats.push({ segmentIndex: segment.index, passengerIndex, holdId: hold.id });
        });
      });
      const created = await api.post('/api/bookings', {
        journeyKey: journey.key,
        passengers: passengers.map((p) => ({ ...p, age: Number(p.age) })),
        seats,
      });
      setBooking(created);
      setStep(4);
    } catch (err) {
      setError(err.message);
    } finally {
      setWorking(false);
    }
  }

  async function pay() {
    setWorking(true);
    setError(null);
    try {
      const result = await api.post(`/api/bookings/${booking.summary.id}/payment`, { method: paymentMethod, instrument });
      setBooking(result);
      setStep(5);
    } catch (err) {
      setError(err.message);
    } finally {
      setWorking(false);
    }
  }

  return (
    <div className="container" style={{ padding: '32px 0 64px' }}>
      <BookingStepper current={step} />
      {error && <p className="banner banner-error" style={{ marginTop: 20 }}>{error}</p>}
      {step >= 2 && step <= 3 && (
        <SelectedClassSummary journey={journey} selectedClasses={selectedClasses} passengerCount={passengerCount} />
      )}

      <div style={{ marginTop: 28 }}>
        {step === 0 && (
          <JourneyOverview
            journey={journey}
            passengerCount={passengerCount}
            onContinue={() => setStep(1)}
          />
        )}

        {step === 1 && (
          <div>
            <h2>Select class</h2>
            <p>Choose the travel class for {passengerCount === 1 ? 'this traveller' : `all ${passengerCount} travellers`} on every part of the journey.</p>
            {journey.segments.map((segment) => (
              <div key={segment.index} style={{ marginBottom: 28 }}>
                <h3 style={{ fontSize: '1.05rem' }}>
                  {segment.mode} &middot; {segment.routeName} &mdash; {segment.board.name} to {segment.alight.name}
                </h3>
                <ClassSelect
                  classes={segment.classes}
                  passengerCount={passengerCount}
                  selectedClass={selectedClasses[segment.index]?.coachClass}
                  onSelect={(offer) => chooseClass(segment, offer)}
                />
              </div>
            ))}
            <div style={{ display: 'flex', gap: 12 }}>
              <button className="btn btn-ghost" onClick={() => setStep(0)}>Back</button>
              <button className="btn btn-primary" disabled={!allSegmentsHaveClass || working} onClick={continueToSeats}>
                {working ? 'Loading seats\u2026' : 'Continue to seats'}
              </button>
            </div>
          </div>
        )}

        {step === 2 && (
          <div>
            <h2>Choose seats</h2>
            <p>Select {passengerCount} {passengerCount === 1 ? 'seat' : 'seats'} on every part of the journey.</p>
            {journey.segments.map((segment) => {
              const chosen = selectedClasses[segment.index];
              const fullMap = seatMaps[segment.index];
              const filteredMap = fullMap
                ? { ...fullMap, coaches: fullMap.coaches.filter((c) => c.coachClass === chosen?.coachClass) }
                : null;
              return (
                <div key={segment.index} style={{ marginBottom: 28 }}>
                  <h3 style={{ fontSize: '1.05rem' }}>
                    {segment.mode} &middot; {segment.routeName} &mdash; {segment.board.name} to {segment.alight.name}
                  </h3>
                  <p className="journey-sub">
                    {chosen ? `${chosen.label} \u00b7 ${formatMoney(chosen.fare)} per traveller` : 'No class selected'}
                    {' '}&middot; Selected: {(holdsBySegment[segment.index] || []).map((h) => h.seatLabel).join(', ') || 'none yet'}
                  </p>
                  {!fullMap ? (
                    <div className="skeleton" style={{ height: 120 }} />
                  ) : filteredMap.coaches.length === 0 ? (
                    <p className="banner banner-error">
                      No {chosen?.label || 'matching'} coach is available on this service right now. Go back and choose another class.
                    </p>
                  ) : (
                    <SeatMap
                      seatMap={filteredMap}
                      selectedIds={(holdsBySegment[segment.index] || []).map((h) => h.seatId)}
                      onToggle={(seat) => toggleSeat(segment, seat)}
                    />
                  )}
                </div>
              );
            })}
            <div style={{ display: 'flex', gap: 12 }}>
              <button className="btn btn-ghost" disabled={working} onClick={backToClassSelection}>
                Change class
              </button>
              <button className="btn btn-primary" disabled={!allSegmentsHaveSeats || working} onClick={() => setStep(3)}>
                Continue to passengers
              </button>
            </div>
          </div>
        )}

        {step === 3 && (
          <PassengerForm
            passengers={passengers}
            setPassengers={setPassengers}
            saved={savedPassengers}
            working={working}
            onBack={() => setStep(2)}
            onSubmit={submitBooking}
          />
        )}

        {step === 4 && booking && (
          <PaymentStep
            booking={booking}
            method={paymentMethod}
            setMethod={setPaymentMethod}
            instrument={instrument}
            setInstrument={setInstrument}
            working={working}
            onPay={pay}
          />
        )}

        {step === 5 && booking && <ConfirmationStep booking={booking} />}
      </div>
    </div>
  );
}

/** Compact recap of the classes chosen so far, visible from seat selection through passenger details. */
function SelectedClassSummary({ journey, selectedClasses, passengerCount }) {
  const chosen = journey.segments.map((s) => selectedClasses[s.index]).filter(Boolean);
  if (chosen.length === 0) return null;
  const total = chosen.reduce((sum, c) => sum + Number(c.fare), 0) * passengerCount;
  return (
    <div className="class-summary-bar">
      {chosen.map((c, i) => (
        <span key={i} className="class-summary-item">
          <span className="class-summary-code">{shortClassLabel(c.coachClass)}</span>
          {formatMoney(c.fare)} / traveller
        </span>
      ))}
      <span className="class-summary-total">{formatMoney(total)} total &middot; {passengerCount} {passengerCount === 1 ? 'traveller' : 'travellers'}</span>
    </div>
  );
}

function JourneyOverview({ journey, passengerCount, onContinue }) {
  return (
    <div className="card" style={{ padding: 28 }}>
      <RouteLine segments={journey.segments} />
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 18, flexWrap: 'wrap', gap: 16 }}>
        <div>
          <h2 style={{ margin: 0 }}>{formatDateTime(journey.departure)}</h2>
          <p className="journey-sub">{journey.segments[0].board.name}</p>
        </div>
        <div style={{ textAlign: 'right' }}>
          <h2 style={{ margin: 0 }}>{formatDateTime(journey.arrival)}</h2>
          <p className="journey-sub">{journey.segments[journey.segments.length - 1].alight.name}</p>
        </div>
      </div>
      <p>{formatDuration(journey.durationMinutes)} total &middot; {journey.transfers} {journey.transfers === 1 ? 'change' : 'changes'}</p>

      {journey.segments.map((segment) => {
        const fares = segment.classes.map((c) => Number(c.fare));
        const fromFare = fares.length > 0 ? Math.min(...fares) : null;
        return (
          <div key={segment.index} className="segment-row">
            <span className="pill pill-ink">{segment.mode}</span>
            <div style={{ flex: 1 }}>
              <strong>{segment.routeName}</strong> &middot; {segment.vehicleName} ({segment.vehicleNumber})
              <div className="journey-sub">
                {segment.board.name} {formatDateTime(segment.departure)} &rarr; {segment.alight.name} {formatDateTime(segment.arrival)}
              </div>
              {segment.status !== 'SCHEDULED' && (
                <span className="pill pill-amber" style={{ marginTop: 6 }}>
                  {segment.status}{segment.delayMinutes ? ` \u2014 ${segment.delayMinutes} min late` : ''}
                </span>
              )}
            </div>
            <div style={{ textAlign: 'right' }}>
              {fromFare != null ? (
                <>
                  <div>{formatMoney(fromFare)}</div>
                  <div className="journey-sub">from &middot; {segment.classes.length} {segment.classes.length === 1 ? 'class' : 'classes'}</div>
                </>
              ) : (
                <div className="journey-sub">No classes available</div>
              )}
            </div>
          </div>
        );
      })}

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 20, paddingTop: 16, borderTop: '1px solid var(--paper-line)' }}>
        <div>
          <strong style={{ fontSize: '1.2rem' }}>{formatMoney(journey.farePerPassenger)}</strong>
          <span className="journey-sub"> starting fare per traveller &times; {passengerCount}</span>
        </div>
        <button className="btn btn-primary" disabled={!journey.bookable} onClick={onContinue}>
          {journey.bookable ? 'Continue to choose class' : 'Not enough seats'}
        </button>
      </div>
    </div>
  );
}

function PassengerForm({ passengers, setPassengers, saved, working, onBack, onSubmit }) {
  function update(index, field, value) {
    setPassengers((prev) => prev.map((p, i) => (i === index ? { ...p, [field]: value } : p)));
  }
  const complete = passengers.every((p) => p.fullName && p.age && p.gender);

  return (
    <div>
      <h2>Passenger details</h2>
      {passengers.map((passenger, index) => (
        <div key={index} className="card" style={{ padding: 20, marginBottom: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <h3 style={{ fontSize: '1rem' }}>Traveller {index + 1}</h3>
            {saved.length > 0 && (
              <select
                onChange={(e) => {
                  const p = saved.find((s) => String(s.id) === e.target.value);
                  if (p) setPassengers((prev) => prev.map((row, i) => (i === index ? { fullName: p.fullName, age: p.age, gender: p.gender, mobile: p.mobile || '' } : row)));
                }}
                defaultValue=""
              >
                <option value="" disabled>Fill from saved traveller&hellip;</option>
                {saved.map((s) => (
                  <option key={s.id} value={s.id}>{s.fullName}</option>
                ))}
              </select>
            )}
          </div>
          <div className="search-form-row">
            <div className="field">
              <label>Full name</label>
              <input value={passenger.fullName} onChange={(e) => update(index, 'fullName', e.target.value)} />
            </div>
            <div className="field">
              <label>Age</label>
              <input type="number" min="1" max="120" value={passenger.age} onChange={(e) => update(index, 'age', e.target.value)} />
            </div>
            <div className="field">
              <label>Gender</label>
              <select value={passenger.gender} onChange={(e) => update(index, 'gender', e.target.value)}>
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
            <div className="field">
              <label>Mobile (optional)</label>
              <input value={passenger.mobile} onChange={(e) => update(index, 'mobile', e.target.value)} />
            </div>
          </div>
        </div>
      ))}
      <div style={{ display: 'flex', gap: 12 }}>
        <button className="btn btn-ghost" onClick={onBack}>Back</button>
        <button className="btn btn-primary" disabled={!complete || working} onClick={onSubmit}>
          {working ? 'Creating booking\u2026' : 'Continue to payment'}
        </button>
      </div>
    </div>
  );
}

function PaymentStep({ booking, method, setMethod, instrument, setInstrument, working, onPay }) {
  const expiresAt = booking.summary.holdExpiresAt;
  return (
    <div className="card" style={{ padding: 28, maxWidth: 480 }}>
      <h2>Payment</h2>
      <p className="banner banner-info">
        Development payment &mdash; no real money moves. Any instrument works except one ending in <code>0002</code> or containing "fail".
      </p>
      {expiresAt && <p className="journey-sub">Your seats are held until {new Date(expiresAt).toLocaleTimeString()}.</p>}
      <p style={{ fontSize: '1.3rem', fontFamily: 'var(--font-display)' }}>{formatMoney(booking.summary.totalAmount, booking.summary.currency)}</p>
      <div className="field">
        <label>Payment method</label>
        <select value={method} onChange={(e) => setMethod(e.target.value)}>
          <option value="UPI">UPI</option>
          <option value="CARD">Card</option>
          <option value="NETBANKING">Netbanking</option>
        </select>
      </div>
      <div className="field" style={{ marginTop: 12 }}>
        <label>{method === 'UPI' ? 'UPI ID' : method === 'CARD' ? 'Card number' : 'Bank name'}</label>
        <input value={instrument} onChange={(e) => setInstrument(e.target.value)} placeholder={method === 'UPI' ? 'name@bank' : method === 'CARD' ? '4111 1111 1111 1111' : 'Your bank'} />
      </div>
      <button className="btn btn-primary btn-block" style={{ marginTop: 18 }} disabled={!instrument || working} onClick={onPay}>
        {working ? 'Processing\u2026' : `Pay ${formatMoney(booking.summary.totalAmount, booking.summary.currency)}`}
      </button>
    </div>
  );
}

function ConfirmationStep({ booking }) {
  const navigate = useNavigate();
  return (
    <div className="card confirmation-card">
      <div className="confirmation-badge">&#10003;</div>
      <h2>Booking confirmed</h2>
      <p>Reference <strong>{booking.summary.reference}</strong>. A confirmation has been added to your notifications.</p>
      <div style={{ display: 'flex', gap: 12, justifyContent: 'center', marginTop: 20 }}>
        <button className="btn btn-dark" onClick={() => navigate(`/tickets/${booking.summary.id}`)}>View ticket</button>
        <button className="btn btn-ghost" onClick={() => navigate('/dashboard')}>Go to my trips</button>
      </div>
    </div>
  );
}
