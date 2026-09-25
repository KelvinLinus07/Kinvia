const STEPS = ['Journey', 'Class', 'Seats', 'Passengers', 'Payment', 'Confirmation'];

export default function BookingStepper({ current }) {
  return (
    <div className="stepper" aria-label="Booking progress">
      {STEPS.map((step, index) => (
        <span key={step} style={{ display: 'contents' }}>
          <span className={`step ${index === current ? 'active' : index < current ? 'done' : ''}`}>
            <span className="dot">{index < current ? '\u2713' : index + 1}</span>
            {step}
          </span>
          {index < STEPS.length - 1 && <span className={`bar ${index < current ? 'done' : ''}`} />}
        </span>
      ))}
    </div>
  );
}
