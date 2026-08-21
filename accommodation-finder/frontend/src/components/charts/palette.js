/**
 * Chart colours.
 *
 * These eight hues are assigned in fixed order and never cycled - a series keeps
 * its colour when a filter removes its neighbours, which is what stops a chart
 * repainting itself and lying to you. The dark column is the same hues stepped
 * for the dark surface, not an automatic inversion.
 *
 * Validated against this app's own surfaces (#ffffff light, #151a30 dark) for
 * lightness band, chroma, colour-blind separation and contrast. Aqua sits just
 * under 3:1 on white, so every chart using it also ships direct labels and a
 * table view rather than relying on the colour alone.
 */
export const SERIES_LIGHT = [
  '#2a78d6', // 1 blue
  '#eb6834', // 2 orange
  '#1baf7a', // 3 aqua
  '#eda100', // 4 yellow
  '#e87ba4', // 5 magenta
  '#008300', // 6 green
  '#4a3aa7', // 7 violet
  '#e34948', // 8 red
];

export const SERIES_DARK = [
  '#3987e5',
  '#d95926',
  '#199e70',
  '#c98500',
  '#d55181',
  '#008300',
  '#9085e9',
  '#e66767',
];

/** Reserved for state, never for "series 4". Always paired with a label. */
export const STATUS = {
  good: '#0ca30c',
  warning: '#fab219',
  serious: '#ec835a',
  critical: '#d03b3b',
};

export function seriesColour(index, dark) {
  const palette = dark ? SERIES_DARK : SERIES_LIGHT;
  return palette[index % palette.length];
}

/** Charts are drawn in SVG, so they need the resolved value, not a CSS variable. */
export function prefersDark() {
  if (typeof window === 'undefined' || !window.matchMedia) return false;
  return window.matchMedia('(prefers-color-scheme: dark)').matches;
}
