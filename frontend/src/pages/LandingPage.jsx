import SearchForm from '../components/SearchForm';

export default function LandingPage() {
  return (
    <div>
      <section className="hero">
        <div className="container hero-inner">
          <div className="hero-copy">
            <div className="hero-route" aria-hidden="true">
              <span className="hero-dot" />
              <span className="hero-line" />
              <span className="hero-dot mid" />
              <span className="hero-line" />
              <span className="hero-dot" />
            </div>
            <h1>Every journey is really a few smaller ones, joined up.</h1>
            <p className="hero-lede">
              Kinvia plans the whole trip at once &mdash; a bus to the station, a train across the state, a bus to
              the door &mdash; and books it as one ticket.
            </p>
          </div>
        </div>
        <div className="hero-search container">
          <SearchForm dark />
        </div>
      </section>

      <section className="container features">
        <div className="feature">
          <h3>Trains and buses, together</h3>
          <p>Search once and see every way to get there, not just the vehicles one operator happens to run.</p>
        </div>
        <div className="feature">
          <h3>Plan for what matters to you</h3>
          <p>Sort by the fastest option, the cheapest, the fewest changes, or the most comfortable seat.</p>
        </div>
        <div className="feature">
          <h3>One ticket, every leg</h3>
          <p>Multimodal trips are booked as a single journey, with one reference and one digital ticket.</p>
        </div>
      </section>
    </div>
  );
}
