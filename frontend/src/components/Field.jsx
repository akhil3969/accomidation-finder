import { AnimatePresence, motion } from 'framer-motion';
import { DURATION, EASE } from '../motion/tokens';

/**
 * One labelled form control, with the hint and the live validation message.
 *
 * Wraps whatever control is passed as children so text inputs, selects,
 * textareas and chip pickers all get the same label placement, the same error
 * styling and the same `aria-describedby` wiring.
 */
export default function Field({ id, label, hint, error, required, children, className = '' }) {
  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;

  return (
    <div className={`field ${error ? 'field--invalid' : ''} ${className}`.trim()}>
      {label && (
        <label htmlFor={id}>
          {label}
          {required && (
            <span className="field__required" aria-hidden="true">
              *
            </span>
          )}
        </label>
      )}

      {children({
        id,
        name: id,
        'aria-invalid': error ? 'true' : undefined,
        'aria-describedby': [errorId, hintId].filter(Boolean).join(' ') || undefined,
      })}

      {hint && (
        <span className="field__hint" id={hintId}>
          {hint}
        </span>
      )}

      <AnimatePresence initial={false}>
        {error && (
          <motion.span
            key="error"
            id={errorId}
            className="field__error"
            role="alert"
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: DURATION.fast, ease: EASE }}
          >
            {error}
          </motion.span>
        )}
      </AnimatePresence>
    </div>
  );
}

/**
 * Convenience wrapper for the common `<Field><input/></Field>` case.
 *
 * `className` lands on the field wrapper rather than the control, because that
 * is what grid placement classes like `form-grid--full` need to reach.
 */
export function TextField({ id, label, hint, error, required, className, ...inputProps }) {
  return (
    <Field id={id} label={label} hint={hint} error={error} required={required} className={className}>
      {(a11y) => <input {...a11y} {...inputProps} />}
    </Field>
  );
}

/** Same, for a textarea. */
export function TextAreaField({ id, label, hint, error, required, className, ...textareaProps }) {
  return (
    <Field id={id} label={label} hint={hint} error={error} required={required} className={className}>
      {(a11y) => <textarea {...a11y} {...textareaProps} />}
    </Field>
  );
}
