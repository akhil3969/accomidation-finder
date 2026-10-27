import { Suspense, lazy, useCallback, useEffect, useRef, useState } from 'react';

const GlobeIntro = lazy(() => import('./GlobeIntro'));

const SEEN_KEY = 'accomfinder.introSeen';

/**
 * Decides, once, whether the globe intro plays - and never lets it get in anyone's way:
 * - only when the visit starts on the home page (a shared listing link opens the listing);
 * - only once per browser session;
 * - never for people who asked their device for reduced motion.
 *
 * The site renders underneath the whole time, so its data is already loading;
 * the plain background shows instantly while the globe's code arrives.
 */
function shouldPlay() {
  try {
    if (window.location.pathname !== '/') return false;
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return false;
    // Opened in a background tab: nobody is watching, and browsers pause animation
    // frames there, so it would only sit in front of the page.
    if (document.visibilityState === 'hidden') return false;
    if (window.sessionStorage.getItem(SEEN_KEY)) return false;
    return true;
  } catch {
    return false;
  }
}

export default function IntroGate() {
  const [visible, setVisible] = useState(shouldPlay);
  const [leaving, setLeaving] = useState(false);
  const finished = useRef(false);

  // Skip, any key, a click and the end of the timeline can all race to here.
  const finish = useCallback(() => {
    if (finished.current) return;
    finished.current = true;
    try {
      window.sessionStorage.setItem(SEEN_KEY, '1');
    } catch {
      /* private mode: the intro may simply play again next time */
    }
    setLeaving(true);
    // Matches the .intro fade-out duration in index.css.
    window.setTimeout(() => setVisible(false), 450);
  }, []);

  // A hard cap: whatever happens to the animation (no frames, a slow GPU), the
  // visitor is never kept waiting in front of the site for more than this.
  useEffect(() => {
    if (!visible) return undefined;
    const cap = window.setTimeout(finish, 6000);
    return () => window.clearTimeout(cap);
  }, [visible, finish]);

  // Any key skips: Escape is expected, but nobody should have to hunt for it.
  useEffect(() => {
    if (!visible) return undefined;
    const onKey = () => finish();
    window.addEventListener('keydown', onKey, { once: true });
    return () => window.removeEventListener('keydown', onKey);
  }, [visible, finish]);

  // Nothing behind the intro should scroll while it plays.
  useEffect(() => {
    if (!visible) return undefined;
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previous;
    };
  }, [visible]);

  if (!visible) return null;

  return (
    <div className={`intro ${leaving ? 'is-leaving' : ''}`} onClick={finish}>
      {/* Stays mounted through the fade so the globe dissolves instead of vanishing. */}
      <Suspense fallback={null}>
        <GlobeIntro onDone={finish} />
      </Suspense>
      <button
        type="button"
        className="intro__skip"
        onClick={(event) => {
          event.stopPropagation();
          finish();
        }}
      >
        Skip intro
      </button>
    </div>
  );
}
