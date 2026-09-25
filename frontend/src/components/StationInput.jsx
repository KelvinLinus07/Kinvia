import { useEffect, useRef, useState } from 'react';
import { api, qs } from '../api/client';

/** Type-ahead station picker backed by /api/stations (name, city or code, partial text). */
export default function StationInput({ id, label, value, onChange, placeholder }) {
  const [query, setQuery] = useState(value?.name || '');
  // The stations returned for one particular search text; ignored once the text changes.
  const [answer, setAnswer] = useState({ text: '', stations: [] });
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const boxRef = useRef(null);

  useEffect(() => {
    setQuery(value?.name || '');
  }, [value]);

  useEffect(() => {
    function onClickOutside(event) {
      if (boxRef.current && !boxRef.current.contains(event.target)) setOpen(false);
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  useEffect(() => {
    const text = query.trim();
    if (!text || (value && query === value.name)) return undefined;
    let cancelled = false;
    const handle = setTimeout(() => {
      api
        .get(`/api/stations${qs({ q: text, limit: 8 })}`)
        .then((stations) => {
          if (cancelled) return;
          setAnswer({ text, stations });
          setActive(stations.length > 0 ? 0 : -1);
        })
        .catch(() => !cancelled && setAnswer({ text, stations: [] }));
    }, 150);
    return () => {
      cancelled = true;
      clearTimeout(handle);
    };
  }, [query, value]);

  function choose(station) {
    onChange(station);
    setQuery(station.name);
    setOpen(false);
  }

  function onKeyDown(event) {
    if (event.key === 'Escape') {
      setOpen(false);
    } else if (event.key === 'ArrowDown' && options.length > 0) {
      event.preventDefault();
      setOpen(true);
      setActive((i) => (i + 1) % options.length);
    } else if (event.key === 'ArrowUp' && options.length > 0) {
      event.preventDefault();
      setActive((i) => (i <= 0 ? options.length - 1 : i - 1));
    } else if (event.key === 'Enter' && open && active >= 0 && options[active]) {
      event.preventDefault();
      choose(options[active]);
    }
  }

  const showDropdown = open && query.trim() && !(value && query === value.name);
  const answered = showDropdown && answer.text === query.trim();
  const options = answered ? answer.stations : [];
  const place = value?.city ? [value.city, value.state].filter(Boolean).join(', ') : null;

  return (
    <div className="field station-input" ref={boxRef}>
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        autoComplete="off"
        role="combobox"
        aria-expanded={Boolean(showDropdown)}
        aria-controls={`${id}-options`}
        placeholder={placeholder}
        value={query}
        onChange={(e) => {
          setQuery(e.target.value);
          setOpen(true);
          if (value) onChange(null);
        }}
        onFocus={() => setOpen(true)}
        onKeyDown={onKeyDown}
      />
      {value && value.code && (
        <span className="station-selected">
          <strong>{value.code}</strong>
          {place && <> &middot; {place}</>}
        </span>
      )}
      {showDropdown && (
        <ul className="station-dropdown" id={`${id}-options`} role="listbox">
          {showDropdown && !answered && <li className="station-note">Searching stations...</li>}
          {answered && options.length === 0 && (
            <li className="station-note">No station matches &ldquo;{query.trim()}&rdquo;. Try a city or station code.</li>
          )}
          {options.map((station, index) => (
            <li
              key={station.id}
              role="option"
              aria-selected={index === active}
              className={index === active ? 'station-option station-option-active' : 'station-option'}
              onMouseEnter={() => setActive(index)}
              onMouseDown={() => choose(station)}
            >
              <span className="station-line">
                <span className="station-name">{station.name}</span>
                <span className="station-code">{station.code}</span>
              </span>
              <span className="station-meta">{station.city}, {station.state}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
