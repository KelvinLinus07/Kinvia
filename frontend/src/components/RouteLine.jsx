// Structural device, not decoration: renders the real sequence of boarding/transfer/alighting
// stops for a journey, with each segment drawn in a style that names its transport mode.
export default function RouteLine({ segments }) {
  return (
    <div className="route-line" role="img" aria-label={describeForScreenReader(segments)}>
      <span className="stop" />
      {segments.map((segment, index) => (
        <span key={index} style={{ display: 'contents' }}>
          <span className={`seg ${segment.mode.toLowerCase()}`} />
          <span className={`stop ${index < segments.length - 1 ? 'transfer' : ''}`} />
        </span>
      ))}
    </div>
  );
}

function describeForScreenReader(segments) {
  return segments.map((s) => `${s.mode.toLowerCase()} from ${s.board?.name ?? ''} to ${s.alight?.name ?? ''}`).join(', then ');
}
