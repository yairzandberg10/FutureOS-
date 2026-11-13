// fos-timeline.js: one schedule for the whole film (seconds). The phone rig, the thumb, the keys, the screens,
// the captions and the sound all read their times from here, so a change moves everything together.
window.FOS = window.FOS || {};
(() => {
  'use strict';
  const TL = {
    // 1 (0-6) blueprint
    pen: 0.35, outline: [0.45, 1.9], win: [1.65, 2.35], punch: [2.15, 2.4], nav: [2.35, 3.15], keypad: [3.0, 4.15],
    labels: [3.95, 4.6], dims: [[2.0, 5.05], [2.45, 5.2], [3.15, 5.3]], cap1: [1.0, 5.55],
    // 2 (6-11) volume, three quarters, profile
    fill: [5.55, 6.2], solid: [6.0, 7.25], linesOut: [6.35, 7.7], turn34: 6.2, turnSide: 7.7, dim8: [8.25, 10.35], cap2: [8.3, 10.55],
    // 3 (11-17) the hand, the raise, the screen, the first press
    turnBack: 10.75, handIn: [11.25, 12.45], grip: 12.4, raise: 12.6, screenOn: 14.15, logo: [14.3, 15.2], okBoot: 15.5,
    // 4 (17-23) home: focus is the interface. Every press sits on the 120 BPM grid of the sound bed (quarter-beat steps)
    zoom: 16.25, homeMoves: [[17.75, 'left'], [18.5, 'left'], [19.25, 'down'], [20.0, 'right'], [20.75, 'down'], [21.5, 'up'], [22.25, 'up']], cap4: [18.3, 22.6],
    // 5 (23-29) lock: PIN, then the face
    lockEnd: 23.25, pullBack: 23.45, pin: [[24.5, '2'], [24.75, '5'], [25.0, '8'], [25.25, '0']], merge: 25.55,
    scan: [25.9, 26.7], known: 26.75, unlock: 27.25, backIn: 27.2, cap5: [24.35, 28.6],
    // 6 (29-37) messages, then the map
    openMsgs: 29.25, openChat: 30.25, send: 31.5, reply: 32.5, focusLoc: 33.25, openNav: 34.0, route: [34.5, 35.75], navCard: 35.0, cap6: [29.95, 36.6],
    // 7 (37-45) the library, then the assistant
    backNav: 37.0, toLib: 37.75, openLib: 38.25, openReader: 39.25, pages: [40.25, 41.0], asst: [41.75, 42.0], words: [42.75, 43.95], saved: 44.25,
    cap7: [38.55, 44.6], cap7b: 42.25,
    // 8 (45-50) your colour
    backAsst: 45.0, toSettings: 45.25, openSettings: 45.75, colors: [[46.5, 46.75], [47.25, 47.5], [48.0, 48.25], [48.75, 49.0]], cap8: [46.15, 49.7],
    // 9 (50-58) kosher: everything you need, nothing you do not
    backSettings: 50.0, handOut: [50.25, 51.25], pullOut: 50.1, forbid: [51.5, 51.75, 52.0, 52.25], strikes: [52.75, 53.5, 54.25, 55.0],
    forbidOut: 55.75, cap9: [50.9, 57.2], cap9b: 52.75,
    // 10 (58-66) the phone turns, back to lines, the mark, the name
    spin: 57.3, screenOff: 57.85, backHold: 59.1, dissolve: [60.35, 61.1], linesOff: [61.0, 61.6], logoEnd: [61.65, 62.6], word: [62.45, 63.2], slogan: 63.45, soon: 64.0,
  };
  // every key press in the film, in order: [time, key]. Held presses carry a release time.
  TL.presses = [
    [TL.okBoot, 'ok'],
    ...TL.homeMoves.map(([t, k]) => [t, k]),
    [TL.lockEnd, 'end'],
    ...TL.pin,
    [TL.openMsgs, 'ok'], [TL.openChat, 'ok'], [TL.send, 'ok'], [TL.focusLoc, 'down'], [TL.openNav, 'ok'],
    [TL.backNav, 'softR'], [TL.toLib, 'left'], [TL.openLib, 'ok'], [TL.openReader, 'ok'], [TL.pages[0], 'left'], [TL.pages[1], 'left'],
    [TL.asst[0], 'ok'], [TL.asst[1], 'ok'],
    [TL.backAsst, 'softR'], [TL.toSettings, 'left'], [TL.openSettings, 'ok'],
    ...TL.colors.flatMap(([d, o]) => [[d, 'down'], [o, 'ok']]),
    [TL.backSettings, 'softR'],
  ];
  FOS.TL = TL;
})();
