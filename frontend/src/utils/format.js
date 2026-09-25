export function formatTime(isoDateTime) {
  const date = new Date(isoDateTime);
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

export function formatDate(isoDateTime) {
  const date = new Date(isoDateTime);
  return date.toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short' });
}

export function formatDateTime(isoDateTime) {
  return `${formatDate(isoDateTime)}, ${formatTime(isoDateTime)}`;
}

export function formatDuration(totalMinutes) {
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (hours === 0) return `${minutes}m`;
  return minutes === 0 ? `${hours}h` : `${hours}h ${minutes}m`;
}

export function formatMoney(amount, currency = 'INR') {
  const symbol = currency === 'INR' ? '\u20B9' : currency + ' ';
  const value = Number(amount);
  return `${symbol}${value.toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;
}
