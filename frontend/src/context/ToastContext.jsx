import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { toastVariants } from '../motion/tokens';

const ToastContext = createContext(null);

const ICONS = {
  success: '✓',
  error: '⚠',
  info: '•',
};

let nextId = 1;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timersRef = useRef(new Map());

  const dismiss = useCallback((id) => {
    const timer = timersRef.current.get(id);
    if (timer) {
      window.clearTimeout(timer);
      timersRef.current.delete(id);
    }
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const push = useCallback(
    (message, tone = 'info', ttl = 4500) => {
      const id = nextId++;
      // Cap the stack: a burst of live events used to be able to bury the page
      // under a column of toasts taller than the viewport.
      setToasts((current) => [...current, { id, message, tone }].slice(-4));
      timersRef.current.set(id, window.setTimeout(() => dismiss(id), ttl));
      return id;
    },
    [dismiss],
  );

  // Without this, a toast still counting down when the tree unmounts would call
  // setState on a dead component.
  useEffect(() => {
    const timers = timersRef.current;
    return () => {
      timers.forEach((timer) => window.clearTimeout(timer));
      timers.clear();
    };
  }, []);

  const value = useMemo(
    () => ({
      push,
      success: (message) => push(message, 'success'),
      error: (message) => push(message, 'error', 6000),
      info: (message) => push(message, 'info'),
      dismiss,
    }),
    [push, dismiss],
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      {createPortal(
        <div className="toast-stack" role="status" aria-live="polite">
          <AnimatePresence initial={false}>
            {toasts.map((toast) => (
              <motion.div
                key={toast.id}
                layout
                className={`toast toast--${toast.tone}`}
                variants={toastVariants}
                initial="initial"
                animate="animate"
                exit="exit"
              >
                <span className="toast__icon" aria-hidden="true">
                  {ICONS[toast.tone]}
                </span>
                <span className="toast__message">{toast.message}</span>
                <button
                  type="button"
                  className="toast__close"
                  onClick={() => dismiss(toast.id)}
                  aria-label="Dismiss notification"
                >
                  &#10005;
                </button>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>,
        document.body,
      )}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used inside <ToastProvider>');
  }
  return context;
}
