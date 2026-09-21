import { alpha } from "../lib/util";

export const wallpaper = (accent: string): string =>
  `radial-gradient(90% 55% at 80% 6%, ${alpha(accent, 0.42)} 0%, rgba(0,0,0,0) 62%), radial-gradient(85% 50% at 8% 96%, rgba(50,215,75,0.26) 0%, rgba(0,0,0,0) 60%), linear-gradient(180deg, #0c1424 0%, #06080e 55%, #000000 100%)`;
