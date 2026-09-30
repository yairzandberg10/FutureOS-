// reel-edit.js: the cut list of the Instagram reel. The film itself (the body, the screens, the lines, the hand) is the
// launch film's master timeline in fos-timeline.js; this file only says which stretches of it the reel plays, where,
// and how fast. scene.js asks map(t) for the master time of every reel frame, so the picture stays a pure function of t.
//
// Each cut: [reel start s, reel end s, master start s, speed]. Cuts sit on the 120 BPM grid (0.5 s = 15 frames at 30 fps),
// and at speed 2 a master quarter-beat press lands on a reel 16th note (master starts are multiples of 0.25 s). bed.py reads this
// list too, so the sound follows a change here.
window.FOS = window.FOS || {};
(() => {
  'use strict';
  const CUTS = [
    [0.0, 2.5, 0.25, 2],    // the blueprint draws itself
    [2.5, 3.5, 5.5, 2],     // the body fills in and turns
    [3.5, 5.5, 8.0, 1],     // the profile: 8 mm, at the speed it is read
    [5.5, 10.5, 12.5, 2],   // the hand, the screen wakes, the mark draws, home, the focus moves
    [10.5, 13.0, 23.25, 2], // the PIN, the face
    [13.0, 17.0, 28.5, 2],  // messages, the chat, the map
    [17.0, 21.0, 37.0, 2],  // the library, the reader, the assistant
    [21.0, 24.0, 46.0, 1],  // your colour, at the film's speed: one colour wave a second is as fast as a flash rule allows
    [24.0, 27.5, 50.5, 1.5],// everything you need, nothing you do not (1.5: the labels need time, a strike every beat)
    [27.5, 29.5, 57.25, 2], // the phone turns
    [29.5, 33.5, 61.5, 1],  // the mark, the name, the line
  ];
  const END = CUTS[CUTS.length - 1];
  FOS.EDIT = {
    CUTS,
    MDUR: 66.5,   // the master film's length (fos-timeline.js)
    // master time of reel time t
    // a cut falls half a frame early: the motion-blur sub-frames of a frame never straddle two shots (no ghost frame)
    map(t) {
      t += 0.5 / FPS;
      for (const [r0, r1, m0, v] of CUTS) if (t >= r0 && t < r1) return m0 + (t - r0) * v;
      return END[2] + (END[1] - END[0]) * END[3];
    },
    // reel times at which a master time tau is on screen (none when the reel cuts it out)
    cues(tau) {
      const out = [];
      for (const [r0, r1, m0, v] of CUTS) {
        const m1 = m0 + (r1 - r0) * v;
        if (tau >= m0 && tau < m1) out.push(r0 + (tau - m0) / v);
      }
      return out;
    },
  };
})();
