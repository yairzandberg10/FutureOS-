// fos-data.js: every number here comes from a source file, not from taste.
//   body:   hardware/openscad/futureos_family.scad with MODEL = "regular" (mm, SCAD axes: X right, Y up, Z out of the front)
//   UI:     design/FutureOS Design System (tokens/*.css, components/*, FosIcon.jsx)
//   logo:   the user's ftr mark, vectorised from logo-ftr.png (IoU 0.998 against the PNG)
window.FOS = window.FOS || {};
(() => {
  'use strict';
  // ------------------------------------------------------------------ SCAD, MODEL = "regular"
  const D = {};
  D.DIAG_IN = 3.5; D.SCR_AR = 2 / 3;
  D.SCR_W = D.DIAG_IN * 25.4 * D.SCR_AR / Math.sqrt(1 + D.SCR_AR * D.SCR_AR);   // 49.313
  D.SCR_H = D.DIAG_IN * 25.4 / Math.sqrt(1 + D.SCR_AR * D.SCR_AR);              // 73.969
  D.LCD_SIDE = 0.8; D.LCD_BOTTOM = 3.5; D.TOP_BAND = 2.5; D.NAV_GAP = 1.5; D.NAV_H = 20;
  D.ROW_P = 9.5; D.KEY_P = 17; D.BOTTOM_BAND = 5; D.SIDE_WALL = 1.0; D.CORNER_R = 7;
  D.FILLET_BACK = 1.5; D.FILLET_FRONT = 0.6; D.PUNCH_D = 3.2; D.BODY_T = 8;
  D.DIGIT_ROWS = 4;
  D.DIGITS_H = D.DIGIT_ROWS * D.ROW_P;
  D.SCR_OUT_W = D.SCR_W + 2 * D.LCD_SIDE;
  D.SCR_OUT_H = D.SCR_H + D.LCD_SIDE + D.LCD_BOTTOM;
  D.Y_DIGITS = D.BOTTOM_BAND;
  D.Y_NAV = D.Y_DIGITS + D.DIGITS_H;
  D.Y_SCR = D.Y_NAV + D.NAV_H + D.NAV_GAP;
  D.BODY_L = D.Y_SCR + D.SCR_OUT_H + D.TOP_BAND;                               // 145.27
  D.BODY_W = Math.max(D.SCR_OUT_W, 3 * D.KEY_P) + 2 * D.SIDE_WALL;             // 53
  D.CX = D.BODY_W / 2;
  // glass window and the active area
  D.WIN_W = D.BODY_W - 2 * D.SIDE_WALL;
  D.WIN_Y0 = D.Y_SCR; D.WIN_Y1 = D.BODY_L - D.TOP_BAND;
  D.WIN_R_TOP = Math.max(D.CORNER_R - D.SIDE_WALL, 1); D.WIN_R_BOT = 2;
  D.ACT_X0 = D.CX - D.SCR_W / 2; D.ACT_Y0 = D.Y_SCR + D.LCD_BOTTOM;
  D.ACT_X1 = D.ACT_X0 + D.SCR_W; D.ACT_Y1 = D.ACT_Y0 + D.SCR_H;
  D.PUNCH = [D.CX, D.BODY_L - D.TOP_BAND - D.LCD_SIDE - D.PUNCH_D];
  D.EARPIECE = [D.CX, D.BODY_L - D.TOP_BAND / 2, 10, 0.8];
  // keys: [x, y, w, h, shape, name]
  D.digitKeys = [];
  const LABELS = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '0', '#'];
  for (let row = 0; row < D.DIGIT_ROWS; row++) for (let col = 0; col < 3; col++)
    D.digitKeys.push([D.CX + (col - 1) * D.KEY_P, D.Y_DIGITS + (D.DIGIT_ROWS - 1 - row + 0.5) * D.ROW_P,
      D.KEY_P - 2.4, D.ROW_P - 1.6, 'rect', LABELS[row * 3 + col]]);
  D.RING_D = D.NAV_H - 2;
  D.SIDE_KEY_W = D.KEY_P - 2.4; D.SIDE_KEY_H = (D.NAV_H - 3) / 2;
  const NY = D.Y_NAV + D.NAV_H / 2;
  D.navKeys = [
    [D.CX, NY, D.RING_D, D.RING_D, 'ring', 'dpad'],
    [D.CX, NY, D.RING_D * 0.42, D.RING_D * 0.42, 'round', 'ok'],
    [D.CX - D.KEY_P, D.Y_NAV + D.NAV_H * 0.75, D.SIDE_KEY_W, D.SIDE_KEY_H, 'rect', 'softL'],
    [D.CX - D.KEY_P, D.Y_NAV + D.NAV_H * 0.25, D.SIDE_KEY_W, D.SIDE_KEY_H, 'rect', 'call'],
    [D.CX + D.KEY_P, D.Y_NAV + D.NAV_H * 0.75, D.SIDE_KEY_W, D.SIDE_KEY_H, 'rect', 'softR'],
    [D.CX + D.KEY_P, D.Y_NAV + D.NAV_H * 0.25, D.SIDE_KEY_W, D.SIDE_KEY_H, 'rect', 'end'],
  ];
  D.frontKeys = D.navKeys.concat(D.digitKeys);
  D.RING_HOLE = D.RING_D * 0.42 + 1.2;
  D.KP_MARGIN = 0.8; D.KP_RECESS = 0.3; D.KEY_RISE = 1.1; D.GAP = 0.25; D.KEY_ROUND = 0.8;
  {
    const ks = D.frontKeys;
    D.KP = [Math.min(...ks.map(k => k[0] - k[2] / 2)), Math.min(...ks.map(k => k[1] - k[3] / 2)),
      Math.max(...ks.map(k => k[0] + k[2] / 2)), Math.max(...ks.map(k => k[1] + k[3] / 2))];
  }
  // side keys on the right wall: vol+, vol-, power
  D.VOL_L = 10; D.VOL_H = 2.4; D.VOL_SPACING = 1.6; D.VOL_FROM_TOP = 36; D.VOL_PROUD = 0.6;
  D.PWR_L = 8; D.PWR_GAP = 6; D.LCD_T = 2.8; D.LCD_SINK = 0.2; D.MIDFRAME = 0.4;
  D.BATT_T = 3.4;
  D.VOL_Z = -(D.LCD_SINK + D.LCD_T + D.MIDFRAME) - D.BATT_T / 2;                 // -5.1
  const vc = D.BODY_L - D.VOL_FROM_TOP;
  D.VOL_Y = [vc + (D.VOL_L + D.VOL_SPACING) / 2, vc - (D.VOL_L + D.VOL_SPACING) / 2];
  D.PWR_Y = D.VOL_Y[1] - D.VOL_L / 2 - D.PWR_GAP - D.PWR_L / 2;
  D.sideKeys = [[D.VOL_Y[0], D.VOL_L, 'vol+'], [D.VOL_Y[1], D.VOL_L, 'vol-'], [D.PWR_Y, D.PWR_L, 'power']];
  // back: camera with a raised ring, flash
  D.CAM_XY = [D.BODY_W - 10, D.BODY_L - 5.95];
  D.FLASH_XY = [D.CAM_XY[0] - 8, D.CAM_XY[1]];
  D.CAM_HOLE = 6; D.CAM_BUMP = 0.4;
  // the infinity mark on the back (user request): centred, where the apple sits on an iPhone
  D.INF = { x: D.CX, y: 86, w: 22, stroke: 2.0 };
  // colours from the SCAD
  D.BODY_COLOR = '#1c1c1e'; D.KEYPAD_COLOR = '#2a2a2d';
  FOS.D = D;

  // ------------------------------------------------------------------ UI canvas: 640 x 960 device px on the active area
  const U = {};
  U.W = 640; U.H = 960;
  U.MM = D.SCR_W / U.W;                                   // mm per UI px (the same on both axes: the panel is exactly 2:3)
  U.toMM = (u, v) => [D.ACT_X0 + u * U.MM, D.ACT_Y1 - v * U.MM];
  U.fromMM = (x, y) => [(x - D.ACT_X0) / U.MM, (D.ACT_Y1 - y) / U.MM];
  U.PUNCH = U.fromMM(D.PUNCH[0], D.PUNCH[1]);             // [320, 41.5]
  U.PUNCH_R = D.PUNCH_D / 2 / U.MM;
  // the glass window's top corners (r 6 mm around the window edge) clip the active area's top corners
  U.CORNER_TOP = 64;
  FOS.U = U;

  // ------------------------------------------------------------------ design system tokens (dark theme, device px)
  FOS.T = {
    bg: '#000000', surface: '#1C1C1E', glass: '#2C2C2E', text: '#FFFFFF', avatar: '#3A3A3C',
    accents: ['#FFFFFF', '#64D2FF', '#FF9F0A', '#30D158', '#BF5AF2'],
    accentNames: ['לבן', 'תכלת', 'כתום', 'ירוק', 'סגול'],
    font: "Heebo, sans-serif", display: "Rubik, Heebo, sans-serif", mono: "'JetBrains Mono', monospace",
    size: { header: 68, screenTitle: 40, title: 34, base: 32, dialog: 30, body: 28, summary: 26, label: 24, badge: 20 },
    space: { item: 24, screen: 32 },
    radius: { item: 24, row: 40, card: 32, dialog: 40, main: 44, full: 9999 },
    rowList: 130, rowSetting: 108, topBarBtn: 72,
  };

  // ------------------------------------------------------------------ Future Glyphs (FosIcon.jsx): 24 grid, 1.6 stroke, round caps
  FOS.ICONS = {
    call: 'M5.6 4.2h3.2l1.6 4-2 1.4a10.6 10.6 0 0 0 6 6l1.4-2 4 1.6v3.2a1.6 1.6 0 0 1-1.6 1.6A15.4 15.4 0 0 1 4 5.8a1.6 1.6 0 0 1 1.6-1.6Z',
    call_end: 'M3.4 13.4a17 17 0 0 1 17.2 0l-1.4 2.9-4-.6-.5-2.3a11 11 0 0 0-5.4 0l-.5 2.3-4 .6Z',
    chat: 'M4.4 6.9A2.4 2.4 0 0 1 6.8 4.5h10.4a2.4 2.4 0 0 1 2.4 2.4v7a2.4 2.4 0 0 1-2.4 2.4h-6.1l-4.5 3.4v-3.4A2.4 2.4 0 0 1 4.4 13.9Z',
    contacts: 'M6.4 3.6h11.2a2 2 0 0 1 2 2v12.8a2 2 0 0 1-2 2H6.4a2 2 0 0 1-2-2V5.6a2 2 0 0 1 2-2Z|M12 8.2a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 0 0 0-4.8Z|M8.2 17.4a4 4 0 0 1 7.6 0|M2.6 8h3.8|M2.6 12h3.8|M2.6 16h3.8',
    camera: 'M3.8 8.6a2.1 2.1 0 0 1 2.1-2.1h2.3l1.3-2h5l1.3 2h2.3a2.1 2.1 0 0 1 2.1 2.1v8.3a2.1 2.1 0 0 1-2.1 2.1H5.9a2.1 2.1 0 0 1-2.1-2.1Z|M12 9.2a3.4 3.4 0 1 0 0 6.8 3.4 3.4 0 0 0 0-6.8Z',
    navigation: 'M12 3.6 18.6 19.6 12 16.2 5.4 19.6Z',
    menu_book: 'M12 6.8c-2-1.6-4.8-2-8-1.6v12.6c3.2-.4 6 0 8 1.6 2-1.6 4.8-2 8-1.6V5.2c-3.2-.4-6 0-8 1.6Z|M12 6.8v12.6',
    auto_awesome: 'M10 4.6l1.6 4.4 4.4 1.6-4.4 1.6-1.6 4.4-1.6-4.4L4 10.6l4.4-1.6Z|M17.6 13.6l.8 2.2 2.2.8-2.2.8-.8 2.2-.8-2.2-2.2-.8 2.2-.8Z|M17.4 3.6v3.2|M15.8 5.2H19',
    settings: 'M4 7h9.5|M17.5 7h2.5|M4 12h3.5|M11.5 12h8.5|M4 17h9.5|M17.5 17h2.5|M15.5 4.9a2.1 2.1 0 1 0 0 4.2 2.1 2.1 0 0 0 0-4.2Z|M9.5 9.9a2.1 2.1 0 1 0 0 4.2 2.1 2.1 0 0 0 0-4.2Z|M15.5 14.9a2.1 2.1 0 1 0 0 4.2 2.1 2.1 0 0 0 0-4.2Z',
    alarm: 'M12 6a7 7 0 1 0 0 14 7 7 0 0 0 0-14Z|M12 9.4V13l2.6 1.8|M4.6 5.6 7.6 3|M19.4 5.6 16.4 3',
    calendar_today: 'M3.8 7.4a2.2 2.2 0 0 1 2.2-2.2h12a2.2 2.2 0 0 1 2.2 2.2v10.4a2.2 2.2 0 0 1-2.2 2.2H6a2.2 2.2 0 0 1-2.2-2.2Z|M3.8 10h16.4|M8.4 3.4v3.6|M15.6 3.4v3.6',
    music_note: 'M9.8 17.2V7.2l8.4-2.2v10|M7 14.4a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 0 0 0-5.6Z|M15.4 12.2a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 0 0 0-5.6Z',
    image: 'M3.8 6.6a2.2 2.2 0 0 1 2.2-2.2h12a2.2 2.2 0 0 1 2.2 2.2v10.8a2.2 2.2 0 0 1-2.2 2.2H6a2.2 2.2 0 0 1-2.2-2.2Z|d:8.6 9.2|M4.4 16.8 9.2 12l3.4 3.4 3-3 4.2 4.2',
    lock: 'M5 12.6a2.2 2.2 0 0 1 2.2-2.2h9.6a2.2 2.2 0 0 1 2.2 2.2v5a2.2 2.2 0 0 1-2.2 2.2H7.2A2.2 2.2 0 0 1 5 17.6Z|M8.6 10.4V8.2a3.4 3.4 0 0 1 6.8 0v2.2|d:12 15.1',
    lock_open: 'M5 12.6a2.2 2.2 0 0 1 2.2-2.2h9.6a2.2 2.2 0 0 1 2.2 2.2v5a2.2 2.2 0 0 1-2.2 2.2H7.2A2.2 2.2 0 0 1 5 17.6Z|M8.6 10.4V8.2a3.4 3.4 0 0 1 6.6-1.2|d:12 15.1',
    face: 'M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z|d:9.2 10.6|d:14.8 10.6|M9 14.4a4 4 0 0 0 6 0',
    check: 'M4.8 12.4 9.6 17.2 19.2 6.8',
    mic: 'M12 3.4a2.9 2.9 0 0 0-2.9 2.9v4.8a2.9 2.9 0 0 0 5.8 0V6.3A2.9 2.9 0 0 0 12 3.4Z|M5.6 11.1a6.4 6.4 0 0 0 12.8 0|M12 17.5v3.1|M8.8 20.6h6.4',
    send: 'M20.4 4 3.6 12l16.8 8-3.2-8Z|M17.2 12H6.4',
    add: 'M12 4.8v14.4|M4.8 12h14.4',
    more_vert: 'd:12 4.9|d:12 12|d:12 19.1',
    arrow_forward: 'M5 12h13.6|M13 5.6 19.4 12 13 18.4',
    location_on: 'M12 20.8s-6.2-5.6-6.2-10.6a6.2 6.2 0 0 1 12.4 0c0 5-6.2 10.6-6.2 10.6Z|M12 7.8a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 0 0 0-4.8Z',
    public: 'M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z|M4 12h16|M12 4c2.2 2.2 3.4 5 3.4 8s-1.2 5.8-3.4 8c-2.2-2.2-3.4-5-3.4-8S9.8 6.2 12 4Z',
    storefront: 'M4.2 8.4l1.4-4h12.8l1.4 4v1a2.6 2.6 0 0 1-5.2 0 2.6 2.6 0 0 1-5.2 0 2.6 2.6 0 0 1-5.2 0Z|M5.4 12v7.6h13.2V12|M10 19.6v-4.4h4v4.4',
    groups: 'M12 5.4a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 0 0 0-5.6Z|M6.6 18.6a5.4 5.4 0 0 1 10.8 0|M5.6 8.2a2 2 0 1 0 0 4 2 2 0 0 0 0-4Z|M18.4 8.2a2 2 0 1 0 0 4 2 2 0 0 0 0-4Z|M2.4 17.4A3.6 3.6 0 0 1 6 14.2|M21.6 17.4a3.6 3.6 0 0 0-3.6-3.2',
    play_circle: 'M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z|M10 8.6 15.4 12 10 15.4Z',
    circle: 'M12 5a7 7 0 1 0 0 14 7 7 0 0 0 0-14Z',
    book: 'M6 4.4h11.6v15.2H7.4A1.4 1.4 0 0 1 6 18.2Z|M6 17.4A1.4 1.4 0 0 1 7.4 16h10.2|M10 4.4v6l1.8-1.2 1.8 1.2v-6',
    library_books: 'M7.6 3.8h11a1.6 1.6 0 0 1 1.6 1.6v11a1.6 1.6 0 0 1-1.6 1.6h-11A1.6 1.6 0 0 1 6 16.4v-11a1.6 1.6 0 0 1 1.6-1.6Z|M3.4 7.4v11.4a1.8 1.8 0 0 0 1.8 1.8h11.4|M9.6 8H16|M9.6 11H16|M9.6 14h4',
    auto_stories: 'M3.6 6.2 11 8.6v11.2l-7.4-2.4Z|M11 8.6l6.6-4.8v11.4L11 19.8|M20.4 7v11.6',
    book_open: 'M12 18.6c-2.4-1.6-5.4-1.8-8.6-1.2V6.6c3.2-.6 6.2-.4 8.6 1.2 2.4-1.6 5.4-1.8 8.6-1.2v10.8c-3.2-.6-6.2-.4-8.6 1.2Z|M12 7.8v10.8|M3.4 19.8c3.2-.6 6.2-.2 8.6 1.2 2.4-1.4 5.4-1.8 8.6-1.2',
    bookmark: 'M6.6 4.6h10.8v15.2L12 15.8l-5.4 4Z',
    dehaze: 'M4 7h16|M4 12h16|M4 17h16',
    chevron_left: 'M14.6 5.8 8.4 12l6.2 6.2',
    volume_up: 'M4 9.4h3.4L12 5.6v12.8L7.4 14.6H4Z|M15.3 9.4a3.7 3.7 0 0 1 0 5.2|M17.9 6.8a7.4 7.4 0 0 1 0 10.4',
    keyboard: 'M2.8 7.6a1.6 1.6 0 0 1 1.6-1.6h15.2a1.6 1.6 0 0 1 1.6 1.6v8.8a1.6 1.6 0 0 1-1.6 1.6H4.4a1.6 1.6 0 0 1-1.6-1.6Z|M8 14.8h8|d:6 9.6|d:9.4 9.6|d:12.8 9.6|d:16.2 9.6|d:18 12.4|d:6 12.4',
    // the soft-right key prints Segoe's Undo glyph (SCAD ICONS). Drawn on the same 24 grid, same stroke.
    undo: 'M9.2 13.8 4.6 9.2l4.6-4.6|M4.6 9.2h10.2a4.8 4.8 0 0 1 0 9.6H11',
  };

  // ------------------------------------------------------------------ the ftr logo, vector (units of the 104 x 112 PNG)
  FOS.LOGO = {
    w: 104, h: 112, stroke: 10, color: '#FF3434',
    // pen order: f (stem, arch, stub), t (stem, bowl, stub), r stem, r shoulder, the two crossbars
    strokes: [
      'M5 107V23.5A16.5 16.5 0 0 1 38 23.5V28',
      'M38 35V90.48A16.5 16.5 0 0 0 71 90.48V85.8',
      'M71 80V0',
      'M71 40A33 33 0 0 1 104 7',
      'M10 58H27',
      'M43 58H60',
    ],
  };

  // ------------------------------------------------------------------ copy on the screens (Hebrew, DS voice)
  FOS.COPY = {
    time: '18:40', date: 'יום שלישי, 3 בנובמבר',
    apps: [   // home grid, reading order (right to left, top to bottom). Names from each app's strings.xml
      ['טלפון', 'call'], ['הודעות', 'chat'], ['אנשי קשר', 'contacts'], ['מצלמה', 'camera'],
      ['ניווט ותחבורה', 'navigation'], ['בלכתך בדרך', 'menu_book'], ['עוזרי', 'auto_awesome'], ['הגדרות', 'settings'],
      ['שעון', 'alarm'], ['לוח שנה', 'calendar_today'], ['מוזיקה', 'music_note'], ['גלריה', 'image'],
    ],
    threads: [['דני כהן', 'אתה בדרך?', 1], ['מיכל לוי', 'נתראה בערב', 0], ['נועה ברק', 'תודה רבה', 0], ['אבי מזרחי', 'נדבר מחר', 0]],
    // Pirkei Avot 1:1 to 1:4 (public domain, Sefaria's text without nikud). No divine names on these pages.
    // Pages: [1:1], [1:2, 1:3], [1:4]. Every mishnah is shown whole.
    avot: [
      ['א', 'משה קבל תורה מסיני, ומסרה ליהושע, ויהושע לזקנים, וזקנים לנביאים, ונביאים מסרוה לאנשי כנסת הגדולה. הם אמרו שלשה דברים, הוו מתונים בדין, והעמידו תלמידים הרבה, ועשו סיג לתורה.'],
      ['ב', 'שמעון הצדיק היה משירי כנסת הגדולה. הוא היה אומר, על שלשה דברים העולם עומד, על התורה ועל העבודה ועל גמילות חסדים.'],
      ['ג', 'אנטיגנוס איש סוכו קבל משמעון הצדיק. הוא היה אומר, אל תהיו כעבדים המשמשין את הרב על מנת לקבל פרס, אלא הוו כעבדים המשמשין את הרב שלא על מנת לקבל פרס, ויהי מורא שמים עליכם.'],
      ['ד', 'יוסי בן יועזר איש צרדה ויוסי בן יוחנן איש ירושלים קבלו מהם. יוסי בן יועזר איש צרדה אומר, יהי ביתך בית ועד לחכמים, והוי מתאבק בעפר רגליהם, והוי שותה בצמא את דבריהם.'],
    ],
    avotPages: [[0], [1, 2], [3]],
    assistant: 'תזכיר לי לקנות חלות לשבת',
  };
})();
