/* The single source of truth for timing. The page (video) and music.py (sound) both read it, so a key that is pressed
   on screen is the same key that clicks in the mix. Times are absolute seconds, 120 BPM: beat 0.5 s, bar 2 s, 30 bars.

   Physical keys (see hardware/keyboard_prototype/SPEC.md): up down left right ok soft_l soft_r call end
   d0..d9 star pound.                                                                                              */
(function (root) {
  const BPM = 120, BEAT = 60 / BPM, BAR = BEAT * 4, DUR = 60;
  const keys = [];   // {t, k}   a physical key goes down
  const sfx = [];    // {t, s, ...}   anything else with a sound

  const K = (t, k) => keys.push({ t: +t.toFixed(4), k });
  const S = (t, s, o) => sfx.push(Object.assign({ t: +t.toFixed(4), s }, o || {}));

  /* ---------------------------------------------------------------- 0 - 3   the noise */
  // twenty notifications, faster and faster
  const pings = [0.0, 0.34, 0.62, 0.86, 1.06, 1.24, 1.4, 1.54, 1.66, 1.77, 1.87, 1.96, 2.05, 2.13, 2.2, 2.27, 2.33, 2.39, 2.44, 2.49];
  pings.forEach((t, i) => S(t, 'ping', { i }));
  S(2.5, 'buzz', { dur: 0.25 });
  S(2.75, 'cut');                      // everything stops
  S(3.0, 'boom');                      // the dialog lands

  /* ---------------------------------------------------------------- 3 - 6   the question */
  K(4.0, 'left');                      // focus moves to "delete"
  K(4.5, 'ok');                        // ... and confirms
  S(4.5, 'wipe');
  S(5.0, 'hit', { p: 0 });             // "בלי מסך מגע."
  S(5.5, 'hit', { p: 1 });             // "רק מקשים."

  /* ---------------------------------------------------------------- 6 - 12  the phone */
  S(6.0, 'rise', { dur: 1.2 });
  K(7.0, 'ok');                        // the wordmark is selected
  S(7.0, 'hit', { p: 2 });
  ['d1', 'd2', 'd3', 'd4', 'd5', 'd6', 'd7', 'd8', 'd9', 'star', 'd0', 'pound'].forEach((k, i) => K(7.5 + i * 0.125, k));
  S(8.75, 'push', { dur: 0.6 });
  // the launcher: eight moves that land on the first app
  [['left', 9.75], ['left', 10.0], ['down', 10.25], ['down', 10.5], ['right', 10.75], ['up', 11.0], ['up', 11.25], ['right', 11.5]]
    .forEach(([k, t]) => K(t, k));

  /* ---------------------------------------------------------------- 12 - 36 the tour, one app per bar */
  const T0 = 12, SCENES = 12;
  const open = (j) => K(T0 + j * 2, 'ok');
  for (let j = 0; j < SCENES; j++) open(j);

  // 1 dialer: 0523334455, then the call key
  [0, 5, 2, 3, 3, 3, 4, 4, 5, 5].forEach((d, i) => K(12.25 + i * 0.125, 'd' + d));
  K(13.5, 'call');
  S(13.62, 'ring');

  // 2 T9: היי אני בדרך
  [3, 5, 5].forEach((d, i) => K(14.125 + i * 0.125, 'd' + d));      K(14.5, 'ok');
  [2, 6, 5].forEach((d, i) => K(14.625 + i * 0.125, 'd' + d));      K(15.0, 'ok');
  [2, 3, 8, 5].forEach((d, i) => K(15.125 + i * 0.125, 'd' + d));   K(15.75, 'ok');

  // 3 bluetooth
  K(16.25, 'down'); K(16.5, 'down'); K(16.75, 'down'); K(17.0, 'ok'); S(17.5, 'connect');

  // 4 translate: typed by T9, then translated
  for (let i = 0; i < 12; i++) S(18.2 + i * 0.065, 'type', { i });
  K(19.0, 'ok'); S(19.05, 'translate'); K(19.5, 'left');

  // 5 alarms
  K(20.25, 'down'); K(20.5, 'ok'); K(20.75, 'down'); K(21.0, 'ok');
  S(20.5, 'switch', { on: 1 }); S(21.0, 'switch', { on: 0 });

  // 6 calendar
  K(22.5, 'left'); K(22.75, 'left'); K(23.0, 'left'); K(23.25, 'ok');

  // 7 music
  K(24.5, 'left'); K(25.0, 'ok'); K(25.5, 'down');

  // 8 fitness
  S(26.25, 'beat'); K(27.0, 'down'); K(27.25, 'ok');

  // 9 terminal: what is typed, and when, is data (TL.term) so the ticks match the picture
  const term = [
    { type: 'whoami', t0: 28.15, dt: 0.045 }, { out: 'root', t: 28.5 },
    { type: 'getprop ro.build.version.release', t0: 28.75, dt: 0.03 }, { out: '12', t: 29.8 },
  ];
  term.forEach((l) => { if (l.type) for (let i = 0; i < l.type.length; i++) S(l.t0 + i * l.dt, 'type', { i }); });

  // 10 books
  K(30.5, 'down'); K(30.75, 'down'); K(31.0, 'down'); K(31.5, 'ok');
  for (let i = 0; i < 8; i++) S(30.1 + i * 0.1, 'count', { i });

  // 11 remote
  K(32.25, 'ok'); K(32.75, 'down'); K(33.0, 'down'); S(32.75, 'ir'); S(33.0, 'ir');

  // 12 assistant: a double press of OK
  K(34.25, 'ok'); S(34.5, 'listen');

  /* ---------------------------------------------------------------- 36 - 44 in your colour */
  K(36.25, 'down'); K(36.5, 'down'); K(36.75, 'ok');
  [['down', 37.25], ['ok', 37.5], ['down', 37.75], ['ok', 38.0], ['down', 38.25], ['ok', 38.5], ['down', 38.75], ['ok', 39.0],
   ['up', 39.25], ['up', 39.375], ['up', 39.5], ['ok', 39.625]].forEach(([k, t]) => K(t, k));
  [[37.5, 0], [38.0, 1], [38.5, 2], [39.0, 3], [39.625, 4]].forEach(([t, n]) => S(t, 'chime', { n }));
  K(40.0, 'soft_r'); K(40.25, 'up'); K(40.5, 'up'); K(40.75, 'ok'); K(41.0, 'down'); K(41.25, 'down'); K(41.5, 'ok'); K(43.0, 'ok');
  S(41.5, 'theme', { light: 1 }); S(43.0, 'theme', { light: 0 });
  S(43.6, 'riser', { dur: 0.4 });

  /* ---------------------------------------------------------------- 44 - 54 the list, then the whole keypad */
  K(44.0, 'ok'); K(44.5, 'down'); K(45.0, 'ok'); K(45.5, 'down'); K(46.0, 'ok'); K(46.5, 'down'); K(47.0, 'ok');
  [44.0, 45.0, 46.0, 47.0].forEach((t, i) => S(t, 'switch', { on: i === 3 ? 1 : 0 }));
  S(44.0, 'hit', { p: 3 });
  K(48.0, 'ok');
  // the launcher at full speed: sixteen moves on the eighth notes
  const hop = ['left', 'left', 'down', 'right', 'down', 'left', 'left', 'down', 'right', 'right', 'up', 'left', 'up', 'right', 'down', 'left'];
  hop.forEach((k, i) => K(48.25 + i * 0.25, k));
  S(52.0, 'push', { dur: 0.8, back: 1 });

  /* ---------------------------------------------------------------- 54 - 60 the ending */
  // every key, top to bottom, on sixteenths
  ['soft_l', 'up', 'soft_r', 'left', 'ok', 'right', 'call', 'down', 'end', 'd1', 'd2', 'd3', 'd4', 'd5', 'd6', 'd7', 'd8', 'd9', 'star', 'd0', 'pound']
    .forEach((k, i) => K(54.5 + i * 0.0625, k));
  K(56.0, 'ok'); S(56.0, 'hit', { p: 4 });
  K(57.0, 'ok'); K(58.0, 'ok');           // the phone keeps the pulse while the tagline lands
  S(58.9, 'tail');
  K(59.0, 'ok');

  // T9 words of scene 2: digits pressed, the candidate list after each digit, the word accepted with OK
  const t9 = [
    { t0: 14.125, commit: 14.5, word: 'היי', cands: [['ה', 'ו', 'ד'], ['הכ', 'הי', 'ול', 'דל'], ['היי', 'הכל', 'הלל', 'וכל', 'דלי']], digits: [3, 5, 5] },
    { t0: 14.625, commit: 15.0, word: 'אני', cands: [['א', 'ב', 'ג'], ['אנ', 'אמ', 'בנ'], ['אני', 'בני', 'אמי', 'גמל']], digits: [2, 6, 5] },
    { t0: 15.125, commit: 15.75, word: 'בדרך', cands: [['ב', 'א', 'ג'], ['בד', 'אד', 'גד'], ['בדר', 'אדר'], ['בדרך']], digits: [2, 3, 8, 5] },
  ];

  const TL = { BPM, BEAT, BAR, DUR, keys: keys.sort((a, b) => a.t - b.t), sfx: sfx.sort((a, b) => a.t - b.t), t9, term };
  if (typeof module !== 'undefined' && module.exports) module.exports = TL;
  else root.TL = TL;
})(typeof window !== 'undefined' ? window : globalThis);
