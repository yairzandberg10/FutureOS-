import { loadFont } from "@remotion/google-fonts/Heebo";

// Heebo is the typeface of the FutureOS design files (design/keyboard-panel).
export const { fontFamily: heebo } = loadFont("normal", {
  weights: ["200", "300", "400", "500", "600", "700", "800"],
  subsets: ["hebrew", "latin"],
});
