import { useState } from 'react';

/**
 * The frame every chart sits in: a title, the chart itself, and a table view.
 *
 * The table is not a nicety. Some of our series colours sit below 3:1 against a
 * white surface, and the rule for that is simple - if colour alone might not
 * carry, an alternative reading of the same data has to exist. It also happens to
 * be what a screen reader gets.
 */
export default function ChartCard({ title, subtitle, children, tableHeaders, tableRows }) {
  const [showTable, setShowTable] = useState(false);

  return (
    <section className="card chart-card">
      <div className="card__header">
        <div>
          <h3>{title}</h3>
          {subtitle && <div className="small muted">{subtitle}</div>}
        </div>
        {tableRows?.length > 0 && (
          <button
            type="button"
            className="btn btn--sm btn--ghost"
            aria-pressed={showTable}
            onClick={() => setShowTable((value) => !value)}
          >
            {showTable ? 'Show chart' : 'Show numbers'}
          </button>
        )}
      </div>

      <div className="card__body">
        {showTable ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  {tableHeaders.map((header) => (
                    <th key={header}>{header}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {tableRows.map((row, index) => (
                  <tr key={index}>
                    {row.map((cell, cellIndex) => (
                      <td key={cellIndex} style={{ fontVariantNumeric: 'tabular-nums' }}>
                        {cell}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          children
        )}
      </div>
    </section>
  );
}
