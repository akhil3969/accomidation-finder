import { useState } from 'react';
import useChartTheme from './useChartTheme';
import { seriesColour } from './palette';

/**
 * Horizontal bars for "how many per category".
 *
 * Horizontal rather than vertical because the labels are words - city names,
 * countries - and words read straight rather than rotated forty-five degrees.
 * Every bar is directly labelled with its value, so nothing depends on the reader
 * estimating a length against an axis.
 */
export default function BarChart({ data = [], colourIndex = 0, unit = '', maxBars = 10 }) {
  const theme = useChartTheme();
  const [hover, setHover] = useState(null);

  const rows = data.slice(0, maxBars);
  if (!rows.length) {
    return <p className="small muted center">Nothing to show yet.</p>;
  }

  const max = Math.max(...rows.map((row) => row.value), 1);
  const colour = seriesColour(colourIndex, theme.dark);

  return (
    <div className="bar-chart">
      {rows.map((row, index) => {
        const percentage = (row.value / max) * 100;
        return (
          <div
            key={row.label}
            className={`bar-row ${hover === index ? 'is-hover' : ''}`}
            onMouseEnter={() => setHover(index)}
            onMouseLeave={() => setHover(null)}
          >
            <span className="bar-row__label" title={row.label}>
              {row.label}
            </span>
            <span className="bar-row__track">
              {/* 4px rounded end, anchored to the baseline at zero */}
              <span
                className="bar-row__fill"
                style={{ width: `${Math.max(percentage, 1.5)}%`, background: colour }}
              />
            </span>
            <span className="bar-row__value">
              {row.value}
              {unit}
            </span>
          </div>
        );
      })}
    </div>
  );
}
