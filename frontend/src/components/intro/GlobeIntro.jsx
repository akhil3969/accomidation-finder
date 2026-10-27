import { useEffect, useRef, useState } from 'react';
import createGlobe from 'cobe';

/**
 * The opening scene: a globe that spins, slows onto Paris, zooms in on a pin and
 * then dissolves into the site. Lazy-loaded (cobe and this file are their own
 * chunk), shown once per visit, and only ever mounted by IntroGate.
 *
 * Everything is driven by one requestAnimationFrame loop and a single timeline
 * function of elapsed time, so the motion is smooth and the whole thing is
 * torn down - loop stopped, WebGL context destroyed - the moment it ends.
 */

const PARIS = [48.8566, 2.3522];

// Timeline, in milliseconds. The site starts fading in (onDone) while the globe
// is still zooming through, so there is never an empty screen in between.
const FADE_IN_END = 600;
const SPIN_END = 2200; // the spin decelerates onto Paris by here
const ZOOM_START = 1500;
const ZOOM_END = 2700;
const CAPTION_AT = 1700;
const FADE_START = 2700;
const HAND_OVER_AT = 2850;
const FADE_END = 3300;

const easeOutCubic = (t) => 1 - (1 - t) ** 3;
const easeInOutCubic = (t) => (t < 0.5 ? 4 * t ** 3 : 1 - (-2 * t + 2) ** 3 / 2);
const clamp01 = (t) => Math.min(1, Math.max(0, t));
const progress = (now, from, to) => clamp01((now - from) / (to - from));

/** cobe's own formula for "put this lat/long at the front of the globe". */
function anglesFor([lat, long]) {
  return [Math.PI - ((long * Math.PI) / 180 - Math.PI / 2), (lat * Math.PI) / 180];
}

function themeColours(dark) {
  return dark
    ? { dark: 1, baseColor: [0.3, 0.3, 0.32], glowColor: [0.12, 0.12, 0.14], markerColor: [0.16, 0.59, 1], mapBrightness: 6 }
    : { dark: 0, baseColor: [1, 1, 1], glowColor: [0.92, 0.92, 0.95], markerColor: [0, 0.4, 0.8], mapBrightness: 6 };
}

export default function GlobeIntro({ onDone }) {
  const canvasRef = useRef(null);
  const [phase, setPhase] = useState('spin'); // spin -> caption -> fade
  const doneRef = useRef(onDone);
  doneRef.current = onDone;

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return undefined;

    const dark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const size = canvas.offsetWidth;
    const [targetPhi, targetTheta] = anglesFor(PARIS);
    // Start most of a turn away, so the globe visibly spins before it settles.
    const startPhi = targetPhi - Math.PI * 1.7;
    const startTheta = 0.15;

    let globe;
    try {
      globe = createGlobe(canvas, {
        devicePixelRatio: dpr,
        width: size * dpr,
        height: size * dpr,
        phi: startPhi,
        theta: startTheta,
        diffuse: 1.2,
        mapSamples: 16000,
        markers: [{ location: PARIS, size: 0.05 }],
        scale: 0.92,
        ...themeColours(dark),
      });
    } catch {
      // No WebGL (old device, disabled GPU): skip straight to the site.
      doneRef.current();
      return undefined;
    }

    let frame = 0;
    let shown = 'spin';
    let handedOver = false;
    const start = performance.now();

    const tick = (now) => {
      const t = now - start;
      const spin = easeOutCubic(progress(t, 0, SPIN_END));
      const zoom = easeInOutCubic(progress(t, ZOOM_START, ZOOM_END));
      const fade = easeInOutCubic(progress(t, FADE_START, FADE_END));

      globe.update({
        phi: startPhi + (targetPhi - startPhi) * spin,
        theta: startTheta + (targetTheta - startTheta) * spin,
        // Kept at or under 1: past that the globe outgrows its square canvas
        // and gets clipped. The real zoom is the CSS transform below.
        scale: 0.92 + zoom * 0.08,
        // The pin grows a little as we arrive, so it reads as "here".
        markers: [{ location: PARIS, size: 0.04 + zoom * 0.025 }],
      });

      // transform and opacity only: the compositor animates them off the main thread.
      canvas.style.transform = `scale(${1 + zoom * 0.7 + fade * 0.35})`;
      canvas.style.opacity = String(progress(t, 0, FADE_IN_END) * (1 - fade));

      const next = t >= FADE_START ? 'fade' : t >= CAPTION_AT ? 'caption' : 'spin';
      if (next !== shown) {
        shown = next;
        setPhase(next);
      }
      if (!handedOver && t >= HAND_OVER_AT) {
        handedOver = true;
        doneRef.current();
      }
      // Keeps running through the hand-over so the zoom-through finishes smoothly;
      // the effect cleanup stops it when IntroGate unmounts us.
      if (t < FADE_END) frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);

    return () => {
      cancelAnimationFrame(frame);
      globe.destroy();
    };
  }, []);

  return (
    <div className={`intro__stage is-${phase}`} aria-hidden="true">
      <canvas ref={canvasRef} className="intro__globe" />
      <div className="intro__caption">
        <strong>AccomFinder</strong>
        <span>Homes in Paris, made simple.</span>
      </div>
    </div>
  );
}
