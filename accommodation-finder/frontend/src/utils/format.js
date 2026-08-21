/** Small formatting helpers shared across pages. */

export const ROOM_TYPE_LABELS = {
  STUDIO: 'Studio',
  SHARED: 'Shared room',
  APARTMENT: 'Apartment',
  HOUSE: 'House',
};

export const STATUS_LABELS = {
  PENDING: 'Pending',
  CONFIRMED: 'Confirmed',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
  COMPLETED: 'Completed',
};

const euro = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0,
});

const euroPrecise = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatPrice(value) {
  if (value === null || value === undefined || value === '') return '-';
  return euro.format(Number(value));
}

export function formatPricePrecise(value) {
  if (value === null || value === undefined || value === '') return '-';
  return euroPrecise.format(Number(value));
}

export function formatDate(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}

export function formatDateTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function timeAgo(value) {
  if (!value) return '';
  const then = new Date(value).getTime();
  if (Number.isNaN(then)) return '';
  const seconds = Math.round((Date.now() - then) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} h ago`;
  const days = Math.round(hours / 24);
  if (days < 30) return `${days} d ago`;
  return formatDate(value);
}

/** Nights between two ISO dates, or 0 when either is missing. */
export function nightsBetween(from, to) {
  if (!from || !to) return 0;
  const diff = new Date(to).getTime() - new Date(from).getTime();
  return diff > 0 ? Math.round(diff / 86400000) : 0;
}

export function todayIso(offsetDays = 0) {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  return date.toISOString().slice(0, 10);
}

export function initials(name) {
  if (!name) return '?';
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('');
}

export function placeholderImage(seed = 'room') {
  return `https://picsum.photos/seed/${encodeURIComponent(seed)}/900/600`;
}
