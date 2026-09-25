export default function SeatMap({ seatMap, selectedIds, onToggle }) {
  return (
    <div className="seat-map">
      {seatMap.coaches.map((coach) => (
        <div key={coach.id} className="card" style={{ padding: 18 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
            <strong>{coach.code} &middot; {coach.classLabel}</strong>
            <span className="journey-sub">{coach.availableSeats} left</span>
          </div>
          <div
            className="coach-grid"
            style={{ gridTemplateColumns: `repeat(${coach.columns + (coach.aisleAfter ? 1 : 0)}, 40px)` }}
          >
            {Array.from({ length: coach.rows }).map((_, row) => (
              <RowOfSeats key={row} coach={coach} row={row} selectedIds={selectedIds} onToggle={onToggle} />
            ))}
          </div>
          <div style={{ display: 'flex', gap: 16, marginTop: 14, fontSize: '0.78rem', color: 'var(--text-soft)' }}>
            <Legend swatchClass="" label="Available" />
            <Legend swatchClass="selected" label="Selected" />
            <Legend swatchClass="held" label="Held" />
            <Legend swatchClass="booked" label="Booked" />
          </div>
        </div>
      ))}
    </div>
  );
}

function RowOfSeats({ coach, row, selectedIds, onToggle }) {
  const seatsInRow = coach.seats.filter((s) => s.row === row).sort((a, b) => a.column - b.column);
  const cells = [];
  seatsInRow.forEach((seat) => {
    if (coach.aisleAfter && seat.column === coach.aisleAfter) {
      cells.push(<span key={`aisle-${seat.id}`} className="seat aisle-gap" aria-hidden="true" />);
    }
    const selected = selectedIds.includes(seat.id);
    const disabled = seat.state !== 'AVAILABLE' && !seat.mine;
    cells.push(
      <button
        key={seat.id}
        type="button"
        className={`seat ${selected ? 'selected' : ''} ${seat.state === 'BOOKED' ? 'booked' : ''} ${
          seat.state === 'HELD' && !seat.mine ? 'held' : ''
        }`}
        disabled={disabled && !selected}
        onClick={() => onToggle(seat)}
        title={`Seat ${seat.label}`}
      >
        {seat.label}
      </button>,
    );
  });
  return <>{cells}</>;
}

function Legend({ swatchClass, label }) {
  return (
    <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <span className={`seat ${swatchClass}`} style={{ width: 16, height: 16, borderRadius: 4 }} />
      {label}
    </span>
  );
}
