import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { roomApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import useForm, { compact, rules } from '../hooks/useForm';
import PageTransition from '../components/PageTransition';
import Loader, { ButtonSpinner } from '../components/Loader';
import Field, { TextAreaField, TextField } from '../components/Field';
import EmptyState from '../components/EmptyState';
import { ROOM_TYPE_LABELS, todayIso } from '../utils/format';

const COMMON_AMENITIES = [
  'Wi-Fi', 'Heating', 'Washing machine', 'Dishwasher', 'Kitchenette', 'Desk',
  'Balcony', 'Garden', 'Elevator', 'Parking', 'Bike storage', 'Study room',
];

const EMPTY = {
  title: '',
  description: '',
  city: '',
  address: '',
  postalCode: '',
  country: 'France',
  price: '',
  deposit: '',
  sizeSqm: '',
  bedrooms: 1,
  bathrooms: 1,
  maxGuests: 1,
  available: true,
  furnished: true,
  billsIncluded: false,
  availableFrom: todayIso(7),
  minStayMonths: 6,
  roomType: 'STUDIO',
  latitude: '',
  longitude: '',
  images: '',
  amenities: [],
};

/**
 * Everything a landlord types is checked here before it leaves the browser.
 *
 * The server validates too, of course, but a rejected save used to come back as
 * one line of text under a form of thirty fields with no indication of which one
 * was wrong.
 */
function validate(values) {
  const badImage = values.images
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .find((line) => rules.url(line));

  return compact({
    title:
      rules.required(values.title, 'A title') ||
      rules.minLength(values.title, 6, 'The title') ||
      rules.maxLength(values.title, 200, 'The title'),
    city: rules.required(values.city, 'A city'),
    postalCode: values.postalCode ? rules.frenchPostcode(values.postalCode) : undefined,
    price:
      rules.required(values.price, 'A monthly rent') ||
      rules.number(values.price, { min: 1, max: 100000, label: 'The rent' }),
    deposit: rules.number(values.deposit, { min: 0, max: 100000, label: 'The deposit' }),
    sizeSqm: rules.number(values.sizeSqm, { min: 1, max: 2000, label: 'The size' }),
    bedrooms: rules.number(values.bedrooms, { min: 0, max: 20, label: 'Bedrooms' }),
    bathrooms: rules.number(values.bathrooms, { min: 0, max: 20, label: 'Bathrooms' }),
    maxGuests: rules.number(values.maxGuests, { min: 1, max: 20, label: 'Max guests' }),
    minStayMonths: rules.number(values.minStayMonths, { min: 0, max: 60, label: 'Minimum stay' }),
    latitude: rules.number(values.latitude, { min: -90, max: 90, label: 'Latitude' }),
    longitude: rules.number(values.longitude, { min: -180, max: 180, label: 'Longitude' }),
    description: rules.maxLength(values.description, 5000, 'The description'),
    images: badImage ? `"${badImage}" is not a valid link` : undefined,
  });
}

function buildPayload(values) {
  const numeric = (value) => (value === '' || value === null ? null : Number(value));
  return {
    title: values.title.trim(),
    description: values.description?.trim() || null,
    city: values.city.trim(),
    address: values.address?.trim() || null,
    postalCode: values.postalCode?.trim() || null,
    country: values.country,
    price: numeric(values.price),
    deposit: numeric(values.deposit),
    sizeSqm: numeric(values.sizeSqm),
    bedrooms: numeric(values.bedrooms),
    bathrooms: numeric(values.bathrooms),
    maxGuests: numeric(values.maxGuests),
    available: values.available,
    furnished: values.furnished,
    billsIncluded: values.billsIncluded,
    availableFrom: values.availableFrom || null,
    minStayMonths: numeric(values.minStayMonths),
    roomType: values.roomType,
    latitude: numeric(values.latitude),
    longitude: numeric(values.longitude),
    images: values.images
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean),
    amenities: values.amenities,
  };
}

export default function RoomEditor() {
  const { id } = useParams();
  const editing = Boolean(id);
  useDocumentTitle(editing ? 'Edit listing' : 'New listing');

  const navigate = useNavigate();
  const toast = useToast();
  const [loading, setLoading] = useState(editing);
  const [loadError, setLoadError] = useState(null);

  const form = useForm({
    initialValues: EMPTY,
    validate,
    onSubmit: async (values) => {
      try {
        const payload = buildPayload(values);
        const saved = editing ? await roomApi.update(id, payload) : await roomApi.create(payload);
        toast.success(editing ? 'Listing updated' : 'Listing published and broadcast live');
        navigate(`/rooms/${saved.id}`);
      } catch (error) {
        const message = toMessage(error);
        toast.error(message);
        throw Object.assign(new Error(message), { userMessage: message });
      }
    },
  });

  const { setValues } = form;

  useEffect(() => {
    if (!editing) return undefined;
    let cancelled = false;
    roomApi
      .byId(id)
      .then((room) => {
        if (cancelled) return;
        setValues({
          ...EMPTY,
          ...room,
          price: room.price ?? '',
          deposit: room.deposit ?? '',
          sizeSqm: room.sizeSqm ?? '',
          latitude: room.latitude ?? '',
          longitude: room.longitude ?? '',
          description: room.description ?? '',
          address: room.address ?? '',
          postalCode: room.postalCode ?? '',
          availableFrom: room.availableFrom || todayIso(7),
          images: (room.images || []).join('\n'),
          amenities: room.amenities || [],
        });
      })
      .catch((requestError) => {
        if (!cancelled) setLoadError(toMessage(requestError));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [editing, id, setValues]);

  function toggleAmenity(amenity) {
    const current = form.values.amenities;
    form.setValue(
      'amenities',
      current.includes(amenity)
        ? current.filter((item) => item !== amenity)
        : [...current, amenity],
    );
  }

  if (loading) {
    return (
      <PageTransition>
        <Loader label="Loading listing…" />
      </PageTransition>
    );
  }

  if (loadError) {
    return (
      <PageTransition className="page page--narrow">
        <EmptyState
          icon="&#9888;"
          title="Could not open that listing"
          description={loadError}
          action={
            <button type="button" className="btn btn--primary" onClick={() => navigate('/dashboard')}>
              Back to dashboard
            </button>
          }
        />
      </PageTransition>
    );
  }

  return (
    <PageTransition className="page page--narrow">
      <div className="page-header">
        <div>
          <h1>{editing ? 'Edit listing' : 'Publish a new listing'}</h1>
          <p>Everything except the title, city, price and type is optional.</p>
        </div>
      </div>

      <form className="card" onSubmit={form.handleSubmit} noValidate>
        <div className="card__body stack">
          <TextField
            id="title"
            label="Title"
            required
            maxLength={200}
            placeholder="Bright studio five minutes from the metro"
            value={form.values.title}
            error={form.errorFor('title')}
            onChange={form.handleChange}
            onBlur={form.handleBlur}
          />

          <TextAreaField
            id="description"
            label="Description"
            rows={5}
            value={form.values.description || ''}
            error={form.errorFor('description')}
            onChange={form.handleChange}
            onBlur={form.handleBlur}
          />

          <div className="form-grid">
            <TextField
              id="city"
              label="City"
              required
              value={form.values.city}
              error={form.errorFor('city')}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />
            <TextField
              id="postalCode"
              label="Postal code"
              inputMode="numeric"
              maxLength={5}
              value={form.values.postalCode || ''}
              error={form.errorFor('postalCode')}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />
            <TextField
              id="address"
              label="Address"
              className="form-grid--full"
              value={form.values.address || ''}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />
          </div>

          <div className="form-grid">
            <TextField
              id="price"
              label="Monthly rent (€)"
              type="number"
              min="1"
              step="10"
              required
              value={form.values.price}
              error={form.errorFor('price')}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />
            <TextField
              id="deposit"
              label="Deposit (€)"
              type="number"
              min="0"
              step="10"
              value={form.values.deposit}
              error={form.errorFor('deposit')}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />
            <Field id="roomType" label="Type" required>
              {(a11y) => (
                <select {...a11y} value={form.values.roomType} onChange={form.handleChange}>
                  {Object.entries(ROOM_TYPE_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              )}
            </Field>
          </div>

          <div className="form-grid">
            <TextField
              id="sizeSqm"
              label="Size (m²)"
              type="number"
              min="1"
              value={form.values.sizeSqm}
              error={form.errorFor('sizeSqm')}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />
            <TextField
              id="bedrooms"
              label="Bedrooms"
              type="number"
              min="0"
              max="20"
              value={form.values.bedrooms}
              error={form.errorFor('bedrooms')}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />
            <TextField
              id="bathrooms"
              label="Bathrooms"
              type="number"
              min="0"
              max="20"
              value={form.values.bathrooms}
              error={form.errorFor('bathrooms')}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />
            <TextField
              id="maxGuests"
              label="Max guests"
              type="number"
              min="1"
              max="20"
              value={form.values.maxGuests}
              error={form.errorFor('maxGuests')}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />
          </div>

          <div className="form-grid">
            <TextField
              id="availableFrom"
              label="Available from"
              type="date"
              value={form.values.availableFrom}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />
            <TextField
              id="minStayMonths"
              label="Minimum stay (months)"
              type="number"
              min="0"
              max="60"
              value={form.values.minStayMonths}
              error={form.errorFor('minStayMonths')}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />
          </div>

          <div className="form-grid">
            <TextField
              id="latitude"
              label="Latitude"
              type="number"
              step="0.000001"
              hint="Needed to show the pin on the map."
              value={form.values.latitude}
              error={form.errorFor('latitude')}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />
            <TextField
              id="longitude"
              label="Longitude"
              type="number"
              step="0.000001"
              value={form.values.longitude}
              error={form.errorFor('longitude')}
              onChange={form.handleChange}
              onBlur={form.handleBlur}
            />
          </div>

          <TextAreaField
            id="images"
            label="Photo URLs (one per line)"
            rows={3}
            placeholder="https://…"
            value={form.values.images}
            error={form.errorFor('images')}
            onChange={form.handleChange}
            onBlur={form.handleBlur}
          />

          <Field id="amenities" label="Amenities">
            {() => (
              <div className="chip-row">
                {COMMON_AMENITIES.map((amenity) => (
                  <button
                    key={amenity}
                    type="button"
                    className={`chip ${form.values.amenities.includes(amenity) ? 'is-active' : ''}`}
                    aria-pressed={form.values.amenities.includes(amenity)}
                    onClick={() => toggleAmenity(amenity)}
                  >
                    {amenity}
                  </button>
                ))}
              </div>
            )}
          </Field>

          <div className="row">
            <label className="checkbox">
              <input
                type="checkbox"
                name="available"
                checked={form.values.available}
                onChange={form.handleChange}
              />
              Available now
            </label>
            <label className="checkbox">
              <input
                type="checkbox"
                name="furnished"
                checked={form.values.furnished}
                onChange={form.handleChange}
              />
              Furnished
            </label>
            <label className="checkbox">
              <input
                type="checkbox"
                name="billsIncluded"
                checked={form.values.billsIncluded}
                onChange={form.handleChange}
              />
              Bills included
            </label>
          </div>

          {form.formError && (
            <div className="form-alert" role="alert">
              {form.formError}
            </div>
          )}
        </div>

        <div className="card__footer">
          <button type="button" className="btn btn--ghost" onClick={() => navigate(-1)}>
            Cancel
          </button>
          <button type="submit" className="btn btn--accent" disabled={form.submitting}>
            {form.submitting && <ButtonSpinner />}
            {form.submitting ? 'Saving…' : editing ? 'Save changes' : 'Publish listing'}
          </button>
        </div>
      </form>
    </PageTransition>
  );
}
