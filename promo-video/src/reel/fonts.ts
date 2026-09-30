import { cancelRender, continueRender, delayRender, staticFile } from "remotion";

// Heebo is the typeface of the FutureOS design files (design/keyboard-panel).
// Loaded from public/fonts so a render never depends on the network.
export const reelFont = "Heebo Reel";

const HEBREW = "U+0307-0308, U+0590-05FF, U+200C-2010, U+20AA, U+25CC, U+FB1D-FB4F";
const LATIN =
  "U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+0304, U+0308, U+0329, U+2000-206F, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF, U+FFFD";

const faces = [
  new FontFace(reelFont, `url(${staticFile("fonts/heebo-hebrew-100-900.woff2")})`, { weight: "100 900", unicodeRange: HEBREW }),
  new FontFace(reelFont, `url(${staticFile("fonts/heebo-latin-100-900.woff2")})`, { weight: "100 900", unicodeRange: LATIN }),
];

const handle = delayRender("Loading Heebo");
Promise.all(faces.map((f) => f.load()))
  .then((loaded) => {
    loaded.forEach((f) => document.fonts.add(f));
    continueRender(handle);
  })
  .catch((err) => cancelRender(err));
