import { useMemo } from 'react';
import Field from './Field';
import { ROOM_TYPE_LABELS } from '../utils/format';

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Newest first' },
  { value: 'price,asc', label: 'Price: low to high' },
  { value: 'price,desc', label: 'Price: high to low' },
  { value: 'sizeSqm,desc', label: 'Largest first' },
];

/**
 * Validates the filter bar as it is typed.
 *
 * A max below the min, or a negative price, used to be sent to the server and
 * silently come back as zero results, which looks like "there is nothing here"
 * rather than "you asked for something impossible".
 */
function validate(filters) {
  const errors = {};
  const min = filters.minPrice === '' ? null : Number(filters.minPrice);
  const max = filters.maxPrice === '' ? null : Number(filters.maxPrice);

  if (min !== null && (Number.isNaN(min) || min < 0)) errors.minPrice = 'Enter a price of 0 or more';
  if (max !== null && (Number.isNaN(max) || max < 0)) errors.maxPrice = 'Enter a price of 0 or more';
  if (!errors.minPrice && !errors.maxPrice && min !== null && max !== null && max < min) {
    errors.maxPrice = 'Maximum must be above the minimum';
  }
  if (filters.minSize !== '' && Number(filters.minSize) < 0) {
    errors.minSize = 'Size cannot be negative';
  }
  return errors;
}

export default function SearchFilters({ filters, cities, onChange, onReset, activeCount = 0 }) {
  const errors = useMemo(() => validate(filters), [filters]);

  const set = (key) => (event) => {
    const target = event.target;
    onChange({ ...filters, [key]: target.type === 'checkbox' ? target.checked : target.value });
  };

  return (
    <section className="filters" aria-label="Filters">
      <Field id="filter-city" label="City">
        {(a11y) => (
          <input
            {...a11y}
            list="city-options"
            type="search"
            placeholder="Any city"
            value={filters.city}
            onChange={set('city')}
          />
        )}
      </Field>
      <datalist id="city-options">
        {cities.map((city) => (
          <option key={city} value={city} />
        ))}
      </datalist>

      <Field id="filter-min" label="Min price (€)" error={errors.minPrice}>
        {(a11y) => (
          <input
            {...a11y}
            type="number"
            min="0"
            step="50"
            inputMode="numeric"
            placeholder="Any"
            value={filters.minPrice}
            onChange={set('minPrice')}
          />
        )}
      </Field>

      <Field id="filter-max" label="Max price (€)" error={errors.maxPrice}>
        {(a11y) => (
          <input
            {...a11y}
            type="number"
            min="0"
            step="50"
            inputMode="numeric"
            placeholder="Any"
            value={filters.maxPrice}
            onChange={set('maxPrice')}
          />
        )}
      </Field>

      <Field id="filter-type" label="Type">
        {(a11y) => (
          <select {...a11y} value={filters.roomType} onChange={set('roomType')}>
            <option value="">Any type</option>
            {Object.entries(ROOM_TYPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        )}
      </Field>

      <Field id="filter-size" label="Min size (m²)" error={errors.minSize}>
        {(a11y) => (
          <input
            {...a11y}
            type="number"
            min="0"
            step="5"
            inputMode="numeric"
            placeholder="Any"
            value={filters.minSize}
            onChange={set('minSize')}
          />
        )}
      </Field>

      <Field id="filter-sort" label="Sort by">
        {(a11y) => (
          <select {...a11y} value={filters.sort} onChange={set('sort')}>
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        )}
      </Field>

      <div className="filters__toggles">
        <label className="checkbox">
          <input type="checkbox" checked={filters.availableOnly} onChange={set('availableOnly')} />
          Available only
        </label>
        <label className="checkbox">
          <input type="checkbox" checked={filters.billsIncluded} onChange={set('billsIncluded')} />
          Bills included
        </label>
      </div>

      <div className="filters__actions">
        <button
          type="button"
          className="btn btn--ghost btn--block"
          onClick={onReset}
          disabled={activeCount === 0}
        >
          {activeCount > 0 ? `Reset (${activeCount})` : 'Reset filters'}
        </button>
      </div>
    </section>
  );
}

export { validate as validateFilters };
