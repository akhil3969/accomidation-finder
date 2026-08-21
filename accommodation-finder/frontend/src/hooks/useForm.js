import { useCallback, useMemo, useRef, useState } from 'react';

/**
 * Small controlled-form helper with live validation.
 *
 * Validation runs on every keystroke so the submit button reflects the real
 * state of the form, but a message is only *shown* once the field has been
 * left or a submit has been attempted. Validating loudly while someone is
 * still typing their email tells them they are wrong before they have had a
 * chance to be right.
 */
export default function useForm({ initialValues, validate, onSubmit }) {
  const [values, setValues] = useState(initialValues);
  const [touched, setTouched] = useState({});
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const validateRef = useRef(validate);
  validateRef.current = validate;

  const errors = useMemo(
    () => (validateRef.current ? validateRef.current(values) || {} : {}),
    [values],
  );

  const isValid = Object.keys(errors).length === 0;

  const setValue = useCallback((name, value) => {
    setFormError(null);
    setValues((current) => ({ ...current, [name]: value }));
  }, []);

  const handleChange = useCallback(
    (event) => {
      const target = event.target;
      setValue(target.name, target.type === 'checkbox' ? target.checked : target.value);
    },
    [setValue],
  );

  const handleBlur = useCallback((event) => {
    const name = typeof event === 'string' ? event : event.target.name;
    setTouched((current) => ({ ...current, [name]: true }));
  }, []);

  /** The error to render for a field, or undefined while it should stay quiet. */
  const errorFor = useCallback(
    (name) => ((touched[name] || submitAttempted) ? errors[name] : undefined),
    [errors, touched, submitAttempted],
  );

  const handleSubmit = useCallback(
    async (event) => {
      event?.preventDefault?.();
      setSubmitAttempted(true);
      setFormError(null);

      const current = validateRef.current ? validateRef.current(values) || {} : {};
      if (Object.keys(current).length) {
        // Focus the first field that is actually wrong rather than making the
        // user hunt for the red text.
        const first = event?.target?.elements?.[Object.keys(current)[0]];
        first?.focus?.();
        return false;
      }

      setSubmitting(true);
      try {
        await onSubmit(values);
        return true;
      } catch (error) {
        setFormError(error?.userMessage || error?.message || 'Something went wrong');
        return false;
      } finally {
        setSubmitting(false);
      }
    },
    [values, onSubmit],
  );

  const reset = useCallback(
    (next = initialValues) => {
      setValues(next);
      setTouched({});
      setSubmitAttempted(false);
      setFormError(null);
    },
    [initialValues],
  );

  return {
    values,
    errors,
    errorFor,
    isValid,
    submitting,
    formError,
    setFormError,
    setValue,
    setValues,
    handleChange,
    handleBlur,
    handleSubmit,
    reset,
  };
}

// ------------------------------------------------------------------ rules

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

export const rules = {
  required: (value, label = 'This field') =>
    value === null || value === undefined || String(value).trim() === ''
      ? `${label} is required`
      : undefined,

  email: (value) => (value && !EMAIL.test(String(value).trim()) ? 'Enter a valid email address' : undefined),

  minLength: (value, length, label = 'This field') =>
    value && String(value).length < length ? `${label} must be at least ${length} characters` : undefined,

  maxLength: (value, length, label = 'This field') =>
    value && String(value).length > length ? `${label} must be ${length} characters or fewer` : undefined,

  number: (value, { min, max, label = 'This value' } = {}) => {
    if (value === '' || value === null || value === undefined) return undefined;
    const parsed = Number(value);
    if (Number.isNaN(parsed)) return `${label} must be a number`;
    if (min !== undefined && parsed < min) return `${label} must be at least ${min}`;
    if (max !== undefined && parsed > max) return `${label} must be ${max} or less`;
    return undefined;
  },

  frenchPostcode: (value) =>
    value && !/^\d{5}$/.test(String(value).trim()) ? 'A French postcode is five digits' : undefined,

  url: (value) => {
    if (!value) return undefined;
    try {
      const parsed = new URL(value);
      return parsed.protocol === 'http:' || parsed.protocol === 'https:'
        ? undefined
        : 'Only http and https links are supported';
    } catch {
      return 'Enter a full link, starting with https://';
    }
  },
};

/** Runs each rule in order and keeps the first complaint. */
export function firstError(...checks) {
  for (const check of checks) {
    if (check) return check;
  }
  return undefined;
}

/** Drops the `undefined` entries so `Object.keys(errors).length` means something. */
export function compact(errors) {
  return Object.fromEntries(Object.entries(errors).filter(([, value]) => Boolean(value)));
}

/** 0-4 strength score used by the register form's meter. */
export function passwordStrength(password = '') {
  if (!password) return { score: 0, label: 'Too short' };
  let score = 0;
  if (password.length >= 8) score += 1;
  if (password.length >= 12) score += 1;
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score += 1;
  if (/\d/.test(password) && /[^A-Za-z0-9]/.test(password)) score += 1;
  const labels = ['Too short', 'Weak', 'Fair', 'Good', 'Strong'];
  return { score, label: labels[score] };
}
