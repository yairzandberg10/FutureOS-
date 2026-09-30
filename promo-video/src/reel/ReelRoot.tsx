import { Composition } from "remotion";
import { Reel } from "./Reel";
import timeline from "./timeline.json";

// A root of its own: reel/render.mjs bundles only the reel, so a render needs no network
// (the long-form promo loads Heebo from Google Fonts; the reel loads it from public/fonts).
export const ReelRoot: React.FC = () => (
  <Composition
    id="FutureOSReel"
    component={Reel}
    durationInFrames={timeline.total}
    fps={timeline.fps}
    width={1080}
    height={1920}
    defaultProps={{ showGuides: false }}
  />
);
