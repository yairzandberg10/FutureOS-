import React from "react";
import { AbsoluteFill } from "remotion";
import { linearTiming, TransitionSeries } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { S01Intro } from "./scenes/S01Intro";
import { S02Hero } from "./scenes/S02Hero";
import { S03Keys } from "./scenes/S03Keys";
import { S04Home } from "./scenes/S04Home";
import { S05Typing } from "./scenes/S05Typing";
import { S06Assistant } from "./scenes/S06Assistant";
import { S07Sfarim } from "./scenes/S07Sfarim";
import { S08Wall } from "./scenes/S08Wall";
import { S09Privacy } from "./scenes/S09Privacy";
import { S10Theme } from "./scenes/S10Theme";
import { S11Outro } from "./scenes/S11Outro";

// 11 scenes (1815 frames) minus 10 fades of 15 frames = 1665 frames, 55.5s at 30fps.
export const Promo: React.FC = () => (
  <AbsoluteFill style={{ backgroundColor: "#000000" }}>
    <TransitionSeries>
      <TransitionSeries.Sequence durationInFrames={120} name="Intro">
        <S01Intro />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: 15 })} />
      <TransitionSeries.Sequence durationInFrames={165} name="Hero">
        <S02Hero />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: 15 })} />
      <TransitionSeries.Sequence durationInFrames={150} name="Keys">
        <S03Keys />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: 15 })} />
      <TransitionSeries.Sequence durationInFrames={180} name="Home">
        <S04Home />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: 15 })} />
      <TransitionSeries.Sequence durationInFrames={225} name="Typing">
        <S05Typing />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: 15 })} />
      <TransitionSeries.Sequence durationInFrames={180} name="Assistant">
        <S06Assistant />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: 15 })} />
      <TransitionSeries.Sequence durationInFrames={165} name="Sfarim">
        <S07Sfarim />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: 15 })} />
      <TransitionSeries.Sequence durationInFrames={150} name="App wall">
        <S08Wall />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: 15 })} />
      <TransitionSeries.Sequence durationInFrames={135} name="Privacy">
        <S09Privacy />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: 15 })} />
      <TransitionSeries.Sequence durationInFrames={180} name="Theme">
        <S10Theme />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: 15 })} />
      <TransitionSeries.Sequence durationInFrames={165} name="Outro">
        <S11Outro />
      </TransitionSeries.Sequence>
    </TransitionSeries>
  </AbsoluteFill>
);
