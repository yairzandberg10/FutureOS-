import React from "react";
import { AbsoluteFill, Audio, Sequence, staticFile } from "remotion";
import { Captions } from "./Captions";
import { Hook } from "./Hook";
import { Outro } from "./Outro";
import { Stage } from "./Stage";
import { Wall } from "./Wall";
import { reelFont } from "./fonts";

// FutureOS, 1080x1920 (9:16) for Instagram Reels and Stories. 26 s, 30 fps, 120 BPM (a beat every 15 frames).
// Instagram covers the top ~250 px and the bottom ~340 px with its own UI: titles stay inside that band.
export const Reel: React.FC<{ showGuides?: boolean }> = ({ showGuides = false }) => (
  <AbsoluteFill style={{ backgroundColor: "#000000", fontFamily: `"${reelFont}", sans-serif`, direction: "rtl" }}>
    <Audio src={staticFile("reel/bed.m4a")} />
    <Sequence durationInFrames={92} name="Hook" layout="none">
      <Hook />
    </Sequence>
    <Stage />
    <Captions />
    <Sequence from={565} durationInFrames={105} name="App wall" layout="none">
      <Wall />
    </Sequence>
    <Sequence from={665} durationInFrames={115} name="Outro" layout="none">
      <Outro />
    </Sequence>
    {showGuides ? (
      <AbsoluteFill style={{ pointerEvents: "none" }}>
        <div style={{ position: "absolute", left: 0, right: 0, top: 0, height: 250, background: "rgba(255,0,80,0.18)" }} />
        <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, height: 340, background: "rgba(255,0,80,0.18)" }} />
        <div style={{ position: "absolute", left: 64, right: 64, top: 250, bottom: 340, outline: "2px dashed rgba(255,0,80,0.7)" }} />
      </AbsoluteFill>
    ) : null}
  </AbsoluteFill>
);
