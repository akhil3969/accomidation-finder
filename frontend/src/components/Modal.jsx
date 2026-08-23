import { useCallback, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { backdropVariants, panelVariants } from '../motion/tokens';

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

// Nested dialogs (a confirm on top of an editor) must not each fight over
// body.style.overflow, so the lock is reference counted.
let lockCount = 0;

function lockScroll() {
  if (lockCount === 0) {
    document.body.dataset.scrollLock = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
  }
  lockCount += 1;
}

function unlockScroll() {
  lockCount = Math.max(0, lockCount - 1);
  if (lockCount === 0) {
    document.body.style.overflow = document.body.dataset.scrollLock || '';
    delete document.body.dataset.scrollLock;
  }
}

/**
 * An accessible, animated dialog.
 *
 * It renders through a portal rather than in place because the routed content
 * is inside an animated wrapper, and a CSS transform on an ancestor makes
 * `position: fixed` resolve against that ancestor instead of the viewport - the
 * dialog would sit halfway down the page while it animated.
 *
 * Driven by `open` rather than by the caller conditionally rendering it, so the
 * close animation has something to play out on. The content of the last open
 * render is held while it leaves, so a dialog whose title comes from the state
 * being cleared does not flash empty on the way out.
 */
export default function Modal({ open, title, children, onClose, footer, size = 'md' }) {
  const panelRef = useRef(null);
  const restoreFocusRef = useRef(null);
  const latched = useRef({ title, children, footer });

  if (open) {
    latched.current = { title, children, footer };
  }
  const content = latched.current;

  const handleKeyDown = useCallback(
    (event) => {
      if (event.key === 'Escape') {
        event.stopPropagation();
        onClose?.();
        return;
      }
      if (event.key !== 'Tab') return;

      const nodes = panelRef.current?.querySelectorAll(FOCUSABLE);
      if (!nodes?.length) return;
      const first = nodes[0];
      const last = nodes[nodes.length - 1];

      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    },
    [onClose],
  );

  useEffect(() => {
    if (!open) return undefined;

    restoreFocusRef.current = document.activeElement;
    lockScroll();

    // Wait for the panel to exist before moving focus into it.
    const frame = window.requestAnimationFrame(() => {
      const target = panelRef.current?.querySelector(FOCUSABLE) || panelRef.current;
      target?.focus?.();
    });

    return () => {
      window.cancelAnimationFrame(frame);
      unlockScroll();
      restoreFocusRef.current?.focus?.();
    };
  }, [open]);

  return createPortal(
    <AnimatePresence>
      {open && (
        <motion.div
          className="modal-backdrop"
          role="presentation"
          variants={backdropVariants}
          initial="initial"
          animate="animate"
          exit="exit"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) onClose?.();
          }}
          onKeyDown={handleKeyDown}
        >
          <motion.div
            ref={panelRef}
            className={`modal modal--${size}`}
            role="dialog"
            aria-modal="true"
            aria-label={content.title}
            tabIndex={-1}
            variants={panelVariants}
            initial="initial"
            animate="animate"
            exit="exit"
          >
            <div className="card__header">
              <h3>{content.title}</h3>
              <button type="button" className="icon-btn" onClick={onClose} aria-label="Close dialog">
                &#10005;
              </button>
            </div>
            <div className="card__body">{content.children}</div>
            {content.footer && <div className="card__footer">{content.footer}</div>}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>,
    document.body,
  );
}
