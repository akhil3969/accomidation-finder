// Loaded after first paint by <LazyMotion> in main.jsx. Keeping the animation
// engine out of the entry bundle means the page appears before it arrives;
// until then every <m.*> element simply renders in its final state.
// domAnimation (not domMax): nothing in the app uses layout or drag animations.
import { domAnimation } from 'framer-motion';

export default domAnimation;
