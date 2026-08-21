import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';
import Modal from '../components/Modal';

const ConfirmContext = createContext(null);

/**
 * Replaces `window.confirm` for destructive actions.
 *
 * The native dialog is unstyled, blocks the whole tab, cannot be animated, and
 * on mobile Safari it is easy to dismiss by accident. This gives the same
 * `await confirm(...) -> boolean` ergonomics with a dialog that matches the
 * rest of the app and traps focus properly.
 */
export function ConfirmProvider({ children }) {
  const [request, setRequest] = useState(null);
  const resolverRef = useRef(null);

  const settle = useCallback((answer) => {
    resolverRef.current?.(answer);
    resolverRef.current = null;
    setRequest(null);
  }, []);

  const confirm = useCallback((options) => {
    setRequest(typeof options === 'string' ? { message: options } : options);
    return new Promise((resolve) => {
      resolverRef.current = resolve;
    });
  }, []);

  const value = useMemo(() => ({ confirm }), [confirm]);

  return (
    <ConfirmContext.Provider value={value}>
      {children}
      <Modal
        open={Boolean(request)}
        size="sm"
        title={request?.title || 'Are you sure?'}
        onClose={() => settle(false)}
        footer={
          <>
            <button type="button" className="btn btn--ghost" onClick={() => settle(false)}>
              {request?.cancelLabel || 'Cancel'}
            </button>
            <button
              type="button"
              className={`btn ${request?.tone === 'danger' ? 'btn--danger' : 'btn--primary'}`}
              onClick={() => settle(true)}
            >
              {request?.confirmLabel || 'Confirm'}
            </button>
          </>
        }
      >
        <p style={{ margin: 0 }}>{request?.message}</p>
        {request?.detail && (
          <p className="small muted" style={{ margin: '0.5rem 0 0' }}>
            {request.detail}
          </p>
        )}
      </Modal>
    </ConfirmContext.Provider>
  );
}

export function useConfirm() {
  const context = useContext(ConfirmContext);
  if (!context) {
    throw new Error('useConfirm must be used inside <ConfirmProvider>');
  }
  return context.confirm;
}
