import { useLayoutEffect, useMemo, useRef, useState } from 'react';
import useChartTheme from './useChartTheme';
import { seriesColour } from './palette';

/**
 * Multi-series daily counts.
 *
 * All series are counts of things per day, so they share one axis. If a measure
 * with a different unit ever needs showing, it gets its own chart - a second
 * y-scale would let us draw any relationship we liked.
 */
export default function LineChart({ series = [], height = 260, formatValue = (v) => v }) {
  const theme = useChartTheme();
  const wrapRef = useRef(null);
  const [hover, setHover] = useState(null);

  // Measure the container so the plot fills it. A fixed viewBox letterboxes on a
  // wide screen, which wastes the space that makes a trend readable.
  const [width, setWidth] = useState(720);
  useLayoutEffect(() => {
    const node = wrapRef.current;
    if (!node || typeof ResizeObserver === 'undefined') return undefined;
    const observer = new ResizeObserver(([entry]) => {
      setWidth(Math.max(320, Math.round(entry.contentRect.width)));
    });
    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  const padding = { top: 16, right: 74, bottom: 28, left: 44 };
  const plotWidth = width - padding.left - padding.right;
  const plotHeight = height - padding.top - padding.bottom;

  const { points, labels, ticks } = useMemo(() => {
    const length = series[0]?.data?.length ?? 0;
    const max = Math.max(1, ...series.flatMap((s) => s.data.map((d) => d.value)));
    // A tick count that keeps round numbers on the axis rather than 3.33.
    const step = Math.max(1, Math.ceil(max / 4));
    const top = step * 4;

    const x = (index) => (length <= 1 ? 0 : (index / (length - 1)) * plotWidth);
    const y = (value) => plotHeight - (value / top) * plotHeight;

    return {
      points: series.map((s) => ({
        ...s,
        path: s.data.map((d, i) => `${i === 0 ? 'M' : 'L'} ${x(i)} ${y(d.value)}`).join(' '),
        last: s.data.length ? { x: x(s.data.length - 1), y: y(s.data[s.data.length - 1].value) } : null,
        dots: s.data.map((d, i) => ({ x: x(i), y: y(d.value), ...d })),
      })),
      maxValue: top,
      labels: series[0]?.data?.map((d) => d.date) ?? [],
      ticks: Array.from({ length: 5 }, (_, i) => ({ value: i * step, y: y(i * step) })),
    };
  }, [series, plotWidth, plotHeight]);

  /** Spreads the end-of-line labels vertically so none overlaps its neighbour. */
  const labelPositions = useMemo(() => {
    const minGap = 14;
    const entries = points
      .filter((s) => s.last)
      .map((s) => ({ text: s.label, x: s.last.x, y: s.last.y }))
      .sort((a, b) => a.y - b.y);

    for (let i = 1; i < entries.length; i++) {
      const gap = entries[i].y - entries[i - 1].y;
      if (gap < minGap) {
        entries[i].y = entries[i - 1].y + minGap;
      }
    }
    // If pushing down ran past the bottom, pull the whole stack back up.
    const overflow = entries.length ? entries[entries.length - 1].y - plotHeight : 0;
    if (overflow > 0) {
      entries.forEach((entry) => {
        entry.y -= overflow;
      });
    }
    return entries;
  }, [points, plotHeight]);

  function handleMove(event) {
    const box = wrapRef.current?.getBoundingClientRect();
    if (!box || !labels.length) return;
    const ratio = (event.clientX - box.left) / box.width;
    const svgX = ratio * width - padding.left;
    const index = Math.round((svgX / plotWidth) * (labels.length - 1));
    if (index >= 0 && index < labels.length) {
      setHover(index);
    }
  }

  if (!series.length || !labels.length) {
    return <p className="small muted center">No activity in this period yet.</p>;
  }

  const everyNth = Math.max(1, Math.round(labels.length / 6));

  return (
    <div className="chart" ref={wrapRef} onMouseMove={handleMove} onMouseLeave={() => setHover(null)}>
      <svg viewBox={`0 0 ${width} ${height}`} role="img" width="100%" height={height}
           aria-label={`Daily counts for ${series.map((s) => s.label).join(', ')}`}>
        <g transform={`translate(${padding.left} ${padding.top})`}>
          {/* recessive gridlines, hairline weight */}
          {ticks.map((tick) => (
            <g key={tick.value}>
              <line x1={0} x2={plotWidth} y1={tick.y} y2={tick.y} stroke={theme.grid} strokeWidth="1" />
              <text x={-8} y={tick.y + 4} textAnchor="end" fontSize="11" fill={theme.inkMuted}
                    style={{ fontVariantNumeric: 'tabular-nums' }}>
                {tick.value}
              </text>
            </g>
          ))}

          <line x1={0} x2={plotWidth} y1={plotHeight} y2={plotHeight} stroke={theme.axis} strokeWidth="1" />

          {labels.map((label, index) =>
            index % everyNth === 0 ? (
              <text key={label} x={(index / (labels.length - 1)) * plotWidth} y={plotHeight + 18}
                    textAnchor="middle" fontSize="11" fill={theme.inkMuted}>
                {label.slice(5)}
              </text>
            ) : null,
          )}

          {hover != null && (
            <line
              x1={(hover / (labels.length - 1)) * plotWidth}
              x2={(hover / (labels.length - 1)) * plotWidth}
              y1={0} y2={plotHeight} stroke={theme.axis} strokeWidth="1" strokeDasharray="3 3"
            />
          )}

          {points.map((s, index) => (
            <path key={s.label} d={s.path} fill="none" strokeWidth="2"
                  stroke={seriesColour(index, theme.dark)}
                  strokeLinejoin="round" strokeLinecap="round" />
          ))}

          {/* Direct labels at the line ends - identity without reading the legend, and
              the relief required where a hue is low-contrast on white. Positions are
              spread apart first, because two series ending on similar values would
              otherwise print on top of each other. */}
          {labelPositions.map((label) => (
            <text key={`${label.text}-label`} x={label.x + 8} y={label.y} fontSize="11"
                  fill={theme.inkMuted} dominantBaseline="middle">
              {label.text}
            </text>
          ))}

          {hover != null &&
            points.map((s, index) => {
              const dot = s.dots[hover];
              if (!dot) return null;
              return (
                <circle key={`${s.label}-dot`} cx={dot.x} cy={dot.y} r="4.5"
                        fill={seriesColour(index, theme.dark)}
                        stroke={theme.surface} strokeWidth="2" />
              );
            })}
        </g>
      </svg>

      {hover != null && (
        <div className="chart-tooltip" style={{ left: `${(hover / (labels.length - 1)) * 100}%` }}>
          <strong>{labels[hover]}</strong>
          {points.map((s, index) => (
            <div key={s.label} className="chart-tooltip__row">
              <span className="chart-swatch" style={{ background: seriesColour(index, theme.dark) }} />
              {s.label}
              <span className="spacer" />
              <strong>{formatValue(s.dots[hover]?.value ?? 0)}</strong>
            </div>
          ))}
        </div>
      )}

      <div className="chart-legend">
        {series.map((s, index) => (
          <span key={s.label} className="chart-legend__item">
            <span className="chart-swatch" style={{ background: seriesColour(index, theme.dark) }} />
            {s.label}
          </span>
        ))}
      </div>
    </div>
  );
}
