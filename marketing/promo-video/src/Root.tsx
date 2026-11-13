import { Composition, Folder } from "remotion";
import { Promo } from "./Promo";
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

export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Composition id="FutureOSPromo" component={Promo} durationInFrames={1665} fps={30} width={1920} height={1080} />
      <Folder name="Scenes">
        <Composition id="S01-Intro" component={S01Intro} durationInFrames={120} fps={30} width={1920} height={1080} />
        <Composition id="S02-Hero" component={S02Hero} durationInFrames={165} fps={30} width={1920} height={1080} />
        <Composition id="S03-Keys" component={S03Keys} durationInFrames={150} fps={30} width={1920} height={1080} />
        <Composition id="S04-Home" component={S04Home} durationInFrames={180} fps={30} width={1920} height={1080} />
        <Composition id="S05-Typing" component={S05Typing} durationInFrames={225} fps={30} width={1920} height={1080} />
        <Composition id="S06-Assistant" component={S06Assistant} durationInFrames={180} fps={30} width={1920} height={1080} />
        <Composition id="S07-Sfarim" component={S07Sfarim} durationInFrames={165} fps={30} width={1920} height={1080} />
        <Composition id="S08-Wall" component={S08Wall} durationInFrames={150} fps={30} width={1920} height={1080} />
        <Composition id="S09-Privacy" component={S09Privacy} durationInFrames={135} fps={30} width={1920} height={1080} />
        <Composition id="S10-Theme" component={S10Theme} durationInFrames={180} fps={30} width={1920} height={1080} />
        <Composition id="S11-Outro" component={S11Outro} durationInFrames={165} fps={30} width={1920} height={1080} />
      </Folder>
    </>
  );
};
