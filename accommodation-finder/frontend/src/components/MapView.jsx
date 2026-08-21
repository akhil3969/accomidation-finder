import { useEffect, useMemo } from 'react';
import { MapContainer, Marker, Popup, TileLayer, useMap } from 'react-leaflet';
import L from 'leaflet';
import { Link } from 'react-router-dom';
import { formatPrice } from '../utils/format';

// Leaflet's default marker images break under a bundler, so we inline an SVG pin.
function pin(available) {
  const color = available ? '#0e8a52' : '#cf2617';
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="28" height="38" viewBox="0 0 28 38">
      <path d="M14 0C6.3 0 0 6.3 0 14c0 10 14 24 14 24s14-14 14-24c0-7.7-6.3-14-14-14z" fill="${color}"/>
      <circle cx="14" cy="14" r="5.5" fill="#fff"/>
    </svg>`;
  return L.divIcon({
    html: svg,
    className: 'room-pin',
    iconSize: [28, 38],
    iconAnchor: [14, 38],
    popupAnchor: [0, -34],
  });
}

const PARIS = [48.8566, 2.3522];

/**
 * Keeps the viewport in step with the current result set.
 *
 * `signature` rather than the points array itself: the parent rebuilds that
 * array on every render, so an effect keyed on it re-fitted the bounds
 * continuously - the map visibly twitched whenever anything on the page
 * changed, and it fought the user every time they panned.
 */
function FitBounds({ points, signature }) {
  const map = useMap();

  useEffect(() => {
    if (!points.length) return;
    if (points.length === 1) {
      map.setView(points[0], 14, { animate: true });
      return;
    }
    map.fitBounds(L.latLngBounds(points).pad(0.2), { animate: true });
    // points is derived from signature; including it would defeat the point.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, signature]);

  return null;
}

/** react-leaflet freezes `center`/`zoom` after mount, so moving needs an effect. */
function Recenter({ center, zoom }) {
  const map = useMap();
  const key = center ? `${center[0]},${center[1]},${zoom}` : null;

  useEffect(() => {
    if (!center) return;
    map.setView(center, zoom, { animate: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, key]);

  return null;
}

export default function MapView({ rooms = [], center, zoom = 12, height, scrollZoom = true }) {
  const located = useMemo(
    () => rooms.filter((room) => room.latitude != null && room.longitude != null),
    [rooms],
  );

  const points = useMemo(
    () => located.map((room) => [Number(room.latitude), Number(room.longitude)]),
    [located],
  );

  // A cheap value equality check for the coordinate set.
  const signature = points.map((point) => point.join()).join('|');

  const initialCenter = center || points[0] || PARIS;

  return (
    <MapContainer
      center={initialCenter}
      zoom={zoom}
      scrollWheelZoom={scrollZoom}
      style={height ? { height } : undefined}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {center ? (
        <Recenter center={center} zoom={zoom} />
      ) : (
        <FitBounds points={points} signature={signature} />
      )}

      {located.map((room) => (
        <Marker
          key={room.id}
          position={[Number(room.latitude), Number(room.longitude)]}
          icon={pin(room.available)}
        >
          <Popup>
            <strong>{room.title}</strong>
            {room.city}
            <br />
            {formatPrice(room.price)} / month
            <br />
            <Link to={`/rooms/${room.id}`}>View listing</Link>
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
