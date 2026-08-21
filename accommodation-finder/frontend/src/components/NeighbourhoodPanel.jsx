import { useEffect, useState } from 'react';
import { newcomerApi } from '../api/endpoints';

const ICONS = {
  metro: 'M', train_station: 'R', bus: 'B',
  supermarket: 'S', bakery: 'P', pharmacy: '+',
  hospital: 'H', library: 'L', bank: 'E', laundry: 'W',
};

/**
 * What is actually around the flat.
 *
 * Someone who grew up in the city reads an address and instantly knows whether it
 * is well connected. Someone who arrived last week reads the same address and
 * learns nothing. This is that gap, closed with OpenStreetMap data.
 */
export default function NeighbourhoodPanel({ roomId }) {
  const [info, setInfo] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    newcomerApi
      .neighbourhood(roomId)
      .then((data) => {
        if (!cancelled) setInfo(data);
      })
      .catch(() => {})
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [roomId]);

  if (loading) return <div className="skeleton" style={{ height: 180 }} />;
  if (!info || typeof info !== 'object' || Array.isArray(info)) return null;

  const highlights = info.highlights || [];
  const transport = info.transport || [];
  const essentials = info.essentials || [];

  return (
    <section className="card">
      <div className="card__header">
        <h3>What is around here</h3>
        {info.walkMinutesToTransport != null && (
          <span className="badge badge--info">
            {info.walkMinutesToTransport} min to transport
          </span>
        )}
      </div>

      <div className="card__body stack">
        {info.commute && (
          <div className="spec">
            <span>To {info.commute.destinationName}</span>
            <strong>
              about {info.commute.estimatedMinutes} min by {info.commute.method}
            </strong>
            <span className="small muted">{info.commute.note}</span>
          </div>
        )}

        {highlights.length > 0 && (
          <ul className="note-list">
            {highlights.map((line) => (
              <li key={line}>{line}</li>
            ))}
          </ul>
        )}

        {transport.length > 0 && (
          <>
            <h4 style={{ margin: '0.4rem 0 0', fontSize: '0.9rem' }}>Getting around</h4>
            <div className="stack" style={{ gap: '0.35rem' }}>
              {transport.map((place) => (
                <div key={`${place.type}-${place.name}-${place.metresAway}`} className="row small">
                  <span className="avatar avatar--sm" aria-hidden="true">{ICONS[place.type] || '.'}</span>
                  <span>{place.name}</span>
                  <span className="muted">{place.label}</span>
                  <div className="spacer" />
                  <span className="muted">{place.walkMinutes} min walk</span>
                </div>
              ))}
            </div>
          </>
        )}

        {essentials.length > 0 && (
          <>
            <h4 style={{ margin: '0.4rem 0 0', fontSize: '0.9rem' }}>Day to day</h4>
            <div className="chip-row">
              {essentials.map((place) => (
                <span key={place.type} className="amenity">
                  {place.label} · {place.walkMinutes} min
                </span>
              ))}
            </div>
          </>
        )}

        <p className="small muted" style={{ margin: 0 }}>{info.dataNote}</p>
      </div>
    </section>
  );
}
