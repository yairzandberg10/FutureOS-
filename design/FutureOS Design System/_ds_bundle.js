/* @ds-bundle: {"format":4,"namespace":"FutureOSDesignSystem_3ab611","components":[{"name":"ActionGrid","sourcePath":"components/core/ActionGrid.jsx"},{"name":"Avatar","sourcePath":"components/core/Avatar.jsx"},{"name":"Button","sourcePath":"components/core/Button.jsx"},{"name":"Card","sourcePath":"components/core/Card.jsx"},{"name":"Divider","sourcePath":"components/core/Divider.jsx"},{"name":"EmptyState","sourcePath":"components/core/EmptyState.jsx"},{"name":"FosIcon","sourcePath":"components/core/FosIcon.jsx"},{"name":"FOS_ICON_NAMES","sourcePath":"components/core/FosIcon.jsx"},{"name":"Icon","sourcePath":"components/core/Icon.jsx"},{"name":"IconButton","sourcePath":"components/core/IconButton.jsx"},{"name":"ListItem","sourcePath":"components/core/ListItem.jsx"},{"name":"MonoValue","sourcePath":"components/core/MonoValue.jsx"},{"name":"ScreenHero","sourcePath":"components/core/ScreenHero.jsx"},{"name":"SectionHeader","sourcePath":"components/core/SectionHeader.jsx"},{"name":"TopBar","sourcePath":"components/core/TopBar.jsx"},{"name":"Widget","sourcePath":"components/core/Widget.jsx"},{"name":"Badge","sourcePath":"components/feedback/Badge.jsx"},{"name":"ConfirmDialog","sourcePath":"components/feedback/ConfirmDialog.jsx"},{"name":"GlassPanel","sourcePath":"components/feedback/GlassPanel.jsx"},{"name":"GlassTile","sourcePath":"components/feedback/GlassPanel.jsx"},{"name":"HeadsUpNotification","sourcePath":"components/feedback/HeadsUpNotification.jsx"},{"name":"InputDialog","sourcePath":"components/feedback/InputDialog.jsx"},{"name":"NotificationCard","sourcePath":"components/feedback/NotificationCard.jsx"},{"name":"ProgressBar","sourcePath":"components/feedback/ProgressBar.jsx"},{"name":"Snackbar","sourcePath":"components/feedback/Snackbar.jsx"},{"name":"Spinner","sourcePath":"components/feedback/Spinner.jsx"},{"name":"Capsule","sourcePath":"components/forms/Capsule.jsx"},{"name":"Checkbox","sourcePath":"components/forms/Checkbox.jsx"},{"name":"Chip","sourcePath":"components/forms/Chip.jsx"},{"name":"DatePicker","sourcePath":"components/forms/DatePicker.jsx"},{"name":"DayChip","sourcePath":"components/forms/DayChip.jsx"},{"name":"RadioButton","sourcePath":"components/forms/RadioButton.jsx"},{"name":"SettingItem","sourcePath":"components/forms/SettingItem.jsx"},{"name":"Slider","sourcePath":"components/forms/Slider.jsx"},{"name":"Switch","sourcePath":"components/forms/Switch.jsx"},{"name":"TextArea","sourcePath":"components/forms/TextArea.jsx"},{"name":"TextField","sourcePath":"components/forms/TextField.jsx"},{"name":"TimePicker","sourcePath":"components/forms/TimePicker.jsx"},{"name":"ToggleButton","sourcePath":"components/forms/ToggleButton.jsx"},{"name":"BottomNav","sourcePath":"components/navigation/BottomNav.jsx"},{"name":"OptionsMenu","sourcePath":"components/navigation/OptionsMenu.jsx"},{"name":"SoftKeyBar","sourcePath":"components/navigation/SoftKeyBar.jsx"},{"name":"StatusCapsule","sourcePath":"components/navigation/StatusCapsule.jsx"},{"name":"TabRow","sourcePath":"components/navigation/TabRow.jsx"}],"sourceHashes":{"components/core/ActionGrid.jsx":"4ff967e62be6","components/core/Avatar.jsx":"fca60feda10e","components/core/Button.jsx":"7598ed3564fe","components/core/Card.jsx":"d1221204057c","components/core/Divider.jsx":"9ce386a7db14","components/core/EmptyState.jsx":"0190085decc8","components/core/FosIcon.jsx":"4edeecefa94a","components/core/Icon.jsx":"77fa91a06ef0","components/core/IconButton.jsx":"fd182bcf1148","components/core/ListItem.jsx":"08963c64d715","components/core/MonoValue.jsx":"93033b85d235","components/core/ScreenHero.jsx":"270e3e83fef1","components/core/SectionHeader.jsx":"66f7be2cf6f4","components/core/TopBar.jsx":"f6e6440d6faa","components/core/Widget.jsx":"c3175e202f5d","components/feedback/Badge.jsx":"da8a60b9a105","components/feedback/ConfirmDialog.jsx":"98ad1eadd672","components/feedback/GlassPanel.jsx":"d9ef63e151e2","components/feedback/HeadsUpNotification.jsx":"78cb3d5c38fb","components/feedback/InputDialog.jsx":"16ec2899f44f","components/feedback/NotificationCard.jsx":"33bacf72f505","components/feedback/ProgressBar.jsx":"4fa3e02273ad","components/feedback/Snackbar.jsx":"cca72c5417b1","components/feedback/Spinner.jsx":"074c00e9ef4a","components/forms/Capsule.jsx":"91ad95a91ccd","components/forms/Checkbox.jsx":"d1baa228e661","components/forms/Chip.jsx":"b019249ab06e","components/forms/DatePicker.jsx":"a8ea753c9a0f","components/forms/DayChip.jsx":"8708e3b3b07e","components/forms/RadioButton.jsx":"5fecfa82d426","components/forms/SettingItem.jsx":"2320ee21cca5","components/forms/Slider.jsx":"1706cfbc80b2","components/forms/Switch.jsx":"5bcb19b90b79","components/forms/TextArea.jsx":"31e91283ff62","components/forms/TextField.jsx":"83f6aa086b21","components/forms/TimePicker.jsx":"9a7d5903d7e7","components/forms/ToggleButton.jsx":"1bbb9fbba640","components/navigation/BottomNav.jsx":"bc08fdca8729","components/navigation/OptionsMenu.jsx":"5102a72a04d4","components/navigation/SoftKeyBar.jsx":"d3565ae1f0b0","components/navigation/StatusCapsule.jsx":"545be5eea8ea","components/navigation/TabRow.jsx":"740331b451e3","ui_kits/bluetooth/BluetoothScreens.jsx":"f905a806bb14","ui_kits/calls/ScreensA.jsx":"f03eb79a6759","ui_kits/calls/data.js":"cb2d06765e1d","ui_kits/clock/ClockScreens.jsx":"d3a5d1df7cc3","ui_kits/communication/CommScreens.jsx":"62e066d39d0d","ui_kits/settings/SettingsScreens.jsx":"222f45fdf47c","ui_kits/translate/TranslateScreens.jsx":"9a37b194e4bc"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.FutureOSDesignSystem_3ab611 = window.FutureOSDesignSystem_3ab611 || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/core/Button.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* Pill, 88px tall, 16sp/700 (DialogButton geometry).
   Fills are always solid: no element-level opacity, so labels stay at full contrast.
   Idle and focused share the same fill. Focus = a ring in the button's own color, drawn outside the pill after a small gap (outline + offset, never changes layout) and a 1.02 lift. */
const FILL = {
  primary: {
    bg: "var(--fos-accent)",
    fg: "var(--fos-on-accent)",
    fbg: "var(--fos-accent)",
    ffg: "var(--fos-on-accent)",
    ring: "var(--fos-accent)"
  },
  destructive: {
    bg: "var(--fos-danger)",
    fg: "var(--fos-on-accent)",
    fbg: "var(--fos-danger)",
    ffg: "var(--fos-on-accent)",
    ring: "var(--fos-danger)"
  },
  secondary: {
    bg: "var(--fos-text-20)",
    fg: "var(--fos-text)",
    fbg: "var(--fos-text-20)",
    ffg: "var(--fos-text)",
    ring: "var(--fos-text-20)"
  },
  quiet: {
    bg: "var(--fos-text-10)",
    fg: "var(--fos-text)",
    fbg: "var(--fos-text-10)",
    ffg: "var(--fos-text)",
    ring: "var(--fos-text-10)"
  }
};
function Button({
  children,
  variant = "primary",
  focused = false,
  fullWidth = false,
  onClick,
  style,
  ...rest
}) {
  const v = FILL[variant] || FILL.primary;
  const quiet = variant === "quiet";
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    onClick: onClick,
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      fontSize: "var(--fos-size-base)",
      fontWeight: quiet ? "var(--fos-weight-medium)" : "var(--fos-weight-bold)",
      lineHeight: 1.25,
      color: focused ? v.ffg : v.fg,
      background: focused ? v.fbg : v.bg,
      border: "none",
      outline: focused ? "var(--fos-focus-border-control) solid " + v.ring : "var(--fos-focus-border-control) solid transparent",
      outlineOffset: 4,
      transform: focused ? "scale(var(--fos-focus-scale))" : "none",
      borderRadius: "var(--fos-radius-full)",
      padding: "0 var(--fos-space-9)",
      height: 88,
      minWidth: 176,
      boxSizing: "border-box",
      width: fullWidth ? "100%" : "auto",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      cursor: "pointer",
      transition: "background var(--fos-transition-focus), color var(--fos-transition-focus), outline-color var(--fos-transition-focus), transform var(--fos-transition-focus)",
      ...style
    }
  }, rest), children);
}
Object.assign(__ds_scope, { Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Button.jsx", error: String((e && e.message) || e) }); }

// components/core/Card.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const isRow = n => React.isValidElement(n) && n.type && n.type.__fosCardRow;
function countRows(nodes) {
  let n = 0;
  React.Children.forEach(nodes, node => {
    if (!React.isValidElement(node)) return;
    if (isRow(node)) n += 1;else if (node.props && node.props.children) n += countRows(node.props.children);
  });
  return n;
}
function placeRows(nodes, total, state) {
  return React.Children.map(nodes, node => {
    if (!React.isValidElement(node)) return node;
    if (isRow(node)) {
      const i = state.i++;
      if (node.props.position) return node;
      const pos = total === 1 ? "only" : i === 0 ? "first" : i === total - 1 ? "last" : "middle";
      return React.cloneElement(node, {
        position: pos
      });
    }
    if (node.props && node.props.children) return React.cloneElement(node, {
      children: placeRows(node.props.children, total, state)
    });
    return node;
  });
}

/* SettingsCard: filled surface, 22dp radius, 16dp side inset, 6dp vertical inset,
   4dp shadow in dark / 1dp in light. Rows inside are transparent. */
function Card({
  children,
  style,
  ...rest
}) {
  /* rows learn where they sit, so their focus shape keeps only the card's own corners.
     Rows are found through wrapper elements too — kits wrap them in focus-scroll divs. */
  const count = countRows(children);
  const positioned = count === 0 ? children : placeRows(children, count, {
    i: 0
  });
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      background: "var(--fos-surface-card)",
      borderRadius: "var(--fos-radius-main)",
      boxShadow: "var(--fos-shadow-card)",
      margin: "var(--fos-space-2) var(--fos-space-7)",
      padding: "var(--fos-space-1) 0",
      overflow: "hidden",
      ...style
    }
  }, rest), positioned);
}
Object.assign(__ds_scope, { Card });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Card.jsx", error: String((e && e.message) || e) }); }

// components/core/Divider.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* SettingDivider: 0.8dp hairline in the text color at 12% (dark) / 10% (light),
   inset 16dp from each edge of the card. */
function Divider({
  inset = true,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    role: "separator",
    style: {
      height: "var(--fos-border-divider-row)",
      background: "var(--fos-border-divider)",
      margin: inset ? "0 var(--fos-space-7)" : 0,
      ...style
    }
  }, rest));
}
Object.assign(__ds_scope, { Divider });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Divider.jsx", error: String((e && e.message) || e) }); }

// components/core/Icon.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* Material Symbols Rounded, the set the Kotlin source uses (Icons.Rounded.*).
   size is in device pixels: dp * 2. 18dp = 36px, 22dp = 44px. */
function Icon({
  name,
  size = 40,
  color = "currentColor",
  fill = 0,
  weight = 400,
  opacity,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("span", _extends({
    className: "fos-icon",
    "data-fill": fill,
    "aria-hidden": "true",
    style: {
      fontFamily: '"Material Symbols Rounded"',
      fontSize: size,
      lineHeight: 1,
      width: size,
      height: size,
      flex: "0 0 auto",
      color,
      opacity,
      fontVariationSettings: `"FILL" ${fill}, "wght" ${weight}, "GRAD" 0, "opsz" 24`,
      ...style
    }
  }, rest), name);
}
Object.assign(__ds_scope, { Icon });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Icon.jsx", error: String((e && e.message) || e) }); }

// components/core/FosIcon.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* FutureOS custom icon set — "Future Glyphs".
   One 24-grid, 1.6 stroke, round caps and joins, no fills except the few marks that
   read as solid (star, dots). Drawn from three shapes only: the circle, the 3-radius
   rounded rect, and the 45°/90° line — so the set stays coherent as it grows.
   Unknown names fall through to Material Symbols, so no screen can break. */

const C8 = "M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z";
const SLASH = "M4.4 4.4l15.2 15.2";
const SHIELD = "M12 3.6 5 6.4v5.2c0 4.2 3 7.6 7 8.8 4-1.2 7-4.6 7-8.8V6.4Z";
const BATT = "M6 7h9.8a2.6 2.6 0 0 1 2.6 2.6v4.8a2.6 2.6 0 0 1-2.6 2.6H6a2.6 2.6 0 0 1-2.6-2.6V9.6A2.6 2.6 0 0 1 6 7Z|M20.8 10.4v3.2";
const BELL = "M6.6 16.4V11a5.4 5.4 0 1 1 10.8 0v5.4l1.4 2.2H5.2Z|M10 19.6a2 2 0 0 0 4 0";
const CAM = "M3.8 8.6a2.1 2.1 0 0 1 2.1-2.1h2.3l1.3-2h5l1.3 2h2.3a2.1 2.1 0 0 1 2.1 2.1v8.3a2.1 2.1 0 0 1-2.1 2.1H5.9a2.1 2.1 0 0 1-2.1-2.1Z|M12 9.2a3.4 3.4 0 1 0 0 6.8 3.4 3.4 0 0 0 0-6.8Z";
const FOLDER = "M3.6 7a2 2 0 0 1 2-2h3.6l2.1 2.4h7.1a2 2 0 0 1 2 2v7.6a2 2 0 0 1-2 2H5.6a2 2 0 0 1-2-2Z";
const ARCHIVE_BOX = "M3.6 6.2a1.6 1.6 0 0 1 1.6-1.6h13.6a1.6 1.6 0 0 1 1.6 1.6v1.4a1.6 1.6 0 0 1-1.6 1.6H5.2a1.6 1.6 0 0 1-1.6-1.6Z|M5 9.2v8.8a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V9.2";
const DOC ="M6.8 3.8h6.4L18.4 9v10.8a1.6 1.6 0 0 1-1.6 1.6H6.8a1.6 1.6 0 0 1-1.6-1.6V5.4a1.6 1.6 0 0 1 1.6-1.6Z|M13.2 3.8V9h5.2";
const RECT = "M3.4 6.6a1.8 1.8 0 0 1 1.8-1.8h13.6a1.8 1.8 0 0 1 1.8 1.8v10.8a1.8 1.8 0 0 1-1.8 1.8H5.2a1.8 1.8 0 0 1-1.8-1.8Z";
const CORNERS = "M4 8.4V6a2 2 0 0 1 2-2h2.4|M15.6 4H18a2 2 0 0 1 2 2v2.4|M20 15.6V18a2 2 0 0 1-2 2h-2.4|M8.4 20H6a2 2 0 0 1-2-2v-2.4";
const REPEAT = "M4.6 11V9.4a2 2 0 0 1 2-2h12.2|M16 4.4l3 3-3 3|M19.4 13v1.6a2 2 0 0 1-2 2H5.2|M8 19.6l-3-3 3-3";
const FLASH = "M13.4 3.4 6.4 13h5l-1 7.6 7.2-10h-5.2Z";
const TORCH = "M7.4 3.6h9.2v3.2l-2 3.4v10.2H9.4V10.2l-2-3.4Z|M7.4 6.8h9.2";
const FLAME = "M12 20.4a6 6 0 0 0 6-6c0-3.6-2.8-5.6-3.6-10.2-2.8 1.6-4.4 4.2-4.2 7-1.2-.6-1.8-1.8-2-3-1.6 1.6-2.2 3.8-2.2 6.2a6 6 0 0 0 6 6Z";
const TXT_A = "M3 18.4 7.6 6.6l4.6 11.8|M4.6 14.4h6|M15 12h6";

const P = {
  home: "M3.8 11.2 12 4.2l8.2 7v7.4a1.8 1.8 0 0 1-1.8 1.8H5.6a1.8 1.8 0 0 1-1.8-1.8Z|M9.4 20.4v-5.2h5.2v5.2",
  search: "M10.6 4.6a6 6 0 1 0 0 12 6 6 0 0 0 0-12Z|M15 15l4.6 4.6",
  search_off: "M10.6 4.6a6 6 0 1 0 0 12 6 6 0 0 0 0-12Z|M15 15l4.6 4.6|M7.6 13.6 13.6 7.6",
  settings: "M4 7h9.5|M17.5 7h2.5|M4 12h3.5|M11.5 12h8.5|M4 17h9.5|M17.5 17h2.5|M15.5 4.9a2.1 2.1 0 1 0 0 4.2 2.1 2.1 0 0 0 0-4.2Z|M9.5 9.9a2.1 2.1 0 1 0 0 4.2 2.1 2.1 0 0 0 0-4.2Z|M15.5 14.9a2.1 2.1 0 1 0 0 4.2 2.1 2.1 0 0 0 0-4.2Z",
  arrow_back: "M19 12H5.4|M11 5.6 4.6 12l6.4 6.4",
  arrow_forward: "M5 12h13.6|M13 5.6 19.4 12 13 18.4",
  keyboard_arrow_left: "M14.6 5.8 8.4 12l6.2 6.2",
  chevron_left: "M14.6 5.8 8.4 12l6.2 6.2",
  chevron_right: "M9.4 5.8 15.6 12l-6.2 6.2",
  circle: "M12 5a7 7 0 1 0 0 14 7 7 0 0 0 0-14Z",
  keyboard_arrow_right: "M9.4 5.8 15.6 12l-6.2 6.2",
  keyboard_arrow_down: "M5.8 9.4 12 15.6l6.2-6.2",
  keyboard_arrow_up: "M5.8 14.6 12 8.4l6.2 6.2",
  check: "M4.8 12.4 9.6 17.2 19.2 6.8",
  close: "M6 6l12 12|M18 6 6 18",
  add: "M12 4.8v14.4|M4.8 12h14.4",
  more_vert: "d:12 4.9|d:12 12|d:12 19.1",
  refresh: "M19.2 9.4A7.6 7.6 0 0 0 5.4 8.4|M19.6 4.4v5h-5|M4.8 14.6a7.6 7.6 0 0 0 13.8 1|M4.4 19.6v-5h5",
  restart_alt: "M8.8 6.4A7.6 7.6 0 1 0 12 4.4|M8.6 6.6 12 3.9v5.2",
  edit: "M4.6 19.4h3.3L18.7 8.6a1.7 1.7 0 0 0 0-2.4l-.9-.9a1.7 1.7 0 0 0-2.4 0L4.6 16.1Z|M14.4 6.9l2.7 2.7",
  delete: "M5.4 7h13.2|M9.6 7V5.4A1.4 1.4 0 0 1 11 4h2a1.4 1.4 0 0 1 1.4 1.4V7|M6.9 7l.9 11.2A1.7 1.7 0 0 0 9.5 19.8h5a1.7 1.7 0 0 0 1.7-1.6L17.1 7|M10.6 10.4v6|M13.4 10.4v6",
  info: "M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z|M12 11.2v5.4|d:12 8.1",
  help: "M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z|M9.5 9.7a2.6 2.6 0 1 1 3 2.6v1.4|d:12 17",
  star: "M12 4.2l2.4 5 5.5.8-4 3.9.9 5.5-4.8-2.6-4.8 2.6.9-5.5-4-3.9 5.5-.8Z",
  bookmark: "M6.6 4.6h10.8v15.2L12 15.8l-5.4 4Z",
  call: "M5.6 4.2h3.2l1.6 4-2 1.4a10.6 10.6 0 0 0 6 6l1.4-2 4 1.6v3.2a1.6 1.6 0 0 1-1.6 1.6A15.4 15.4 0 0 1 4 5.8a1.6 1.6 0 0 1 1.6-1.6Z",
  call_end: "M3.4 13.4a17 17 0 0 1 17.2 0l-1.4 2.9-4-.6-.5-2.3a11 11 0 0 0-5.4 0l-.5 2.3-4 .6Z",
  call_received: "M18.8 5.2 8.4 15.6|M8.4 9.6v6h6",
  call_made: "M5.2 18.8 15.6 8.4|M15.6 14.4v-6h-6",
  call_missed: "M19.6 7 12 14.6 4.4 7|M9.4 7H4.4v5",
  ring_volume: "M8.6 8.9a1 1 0 0 0-1.4 0L5.6 10.5a2.2 2.2 0 0 0-.6 2.1 14.5 14.5 0 0 0 8.4 8.4 2.2 2.2 0 0 0 2.1-.6l1.6-1.6a1 1 0 0 0 0-1.4l-2.4-2.4a1 1 0 0 0-1.4 0l-1.1 1.1a11 11 0 0 1-3.8-3.8l1.1-1.1a1 1 0 0 0 0-1.4Z|M12 2.4v2.8|M4.9 4.4 6.9 6.4|M19.1 4.4 17.1 6.4",
  dialpad: "d:7 5|d:12 5|d:17 5|d:7 10|d:12 10|d:17 10|d:7 15|d:12 15|d:17 15|d:12 20",
  contacts: "M6.4 3.6h11.2a2 2 0 0 1 2 2v12.8a2 2 0 0 1-2 2H6.4a2 2 0 0 1-2-2V5.6a2 2 0 0 1 2-2Z|M12 8.2a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 0 0 0-4.8Z|M8.2 17.4a4 4 0 0 1 7.6 0|M2.6 8h3.8|M2.6 12h3.8|M2.6 16h3.8",
  person: "M12 4.2a3.7 3.7 0 1 0 0 7.4 3.7 3.7 0 0 0 0-7.4Z|M5.4 19.8c0-3.7 2.9-6.2 6.6-6.2s6.6 2.5 6.6 6.2",
  mic: "M12 3.4a2.9 2.9 0 0 0-2.9 2.9v4.8a2.9 2.9 0 0 0 5.8 0V6.3A2.9 2.9 0 0 0 12 3.4Z|M5.6 11.1a6.4 6.4 0 0 0 12.8 0|M12 17.5v3.1|M8.8 20.6h6.4",
  mic_off: "M12 3.4a2.9 2.9 0 0 0-2.9 2.9v4.8a2.9 2.9 0 0 0 5.8 0V6.3A2.9 2.9 0 0 0 12 3.4Z|M5.6 11.1a6.4 6.4 0 0 0 12.8 0|M12 17.5v3.1|M4.4 4.4l15.2 15.2",
  volume_up: "M4 9.4h3.4L12 5.6v12.8L7.4 14.6H4Z|M15.3 9.4a3.7 3.7 0 0 1 0 5.2|M17.9 6.8a7.4 7.4 0 0 1 0 10.4",
  volume_off: "M4 9.4h3.4L12 5.6v12.8L7.4 14.6H4Z|M15.6 9.6l4.8 4.8|M20.4 9.6l-4.8 4.8",
  pause: "M8.6 4.8v14.4|M15.4 4.8v14.4",
  play_arrow: "M8.2 5.2 18.4 12 8.2 18.8Z",
  link: "M9.9 14.1a4 4 0 0 1 0-5.6l2.9-2.9a4 4 0 0 1 5.6 5.6l-1.2 1.2|M14.1 9.9a4 4 0 0 1 0 5.6l-2.9 2.9a4 4 0 0 1-5.6-5.6l1.2-1.2",
  link_off: "M9.9 14.1a4 4 0 0 1 0-5.6l1.6-1.6|M14.1 9.9a4 4 0 0 1 0 5.6l-1.6 1.6|M4.4 4.4l15.2 15.2",
  chat: "M4.4 6.9A2.4 2.4 0 0 1 6.8 4.5h10.4a2.4 2.4 0 0 1 2.4 2.4v7a2.4 2.4 0 0 1-2.4 2.4h-6.1l-4.5 3.4v-3.4A2.4 2.4 0 0 1 4.4 13.9Z",
  forum: "M3.6 5.4a1.9 1.9 0 0 1 1.9-1.9h8.2a1.9 1.9 0 0 1 1.9 1.9v4.4a1.9 1.9 0 0 1-1.9 1.9H7.4L3.6 14.4Z|M8.4 14.2v1.2a1.9 1.9 0 0 0 1.9 1.9h6.3l3.8 2.8v-2.9a1.9 1.9 0 0 0 .9-1.6v-3.9a1.9 1.9 0 0 0-1.9-1.9h-1.6",
  bluetooth: "M8.2 7.8 15.8 15.4 12 18.8V5.2l3.8 3.4-7.6 7.6",
  bluetooth_disabled: "M8.2 7.8 15.8 15.4 12 18.8V5.2l3.8 3.4-7.6 7.6|M4.4 4.4l15.2 15.2",
  headphones: "M4.6 14.4v-2a7.4 7.4 0 0 1 14.8 0v2|M3.6 15.4a1.8 1.8 0 0 1 1.8-1.8h1.6v6.2H5.4a1.8 1.8 0 0 1-1.8-1.8Z|M20.4 15.4a1.8 1.8 0 0 0-1.8-1.8H17v6.2h1.6a1.8 1.8 0 0 0 1.8-1.8Z",
  speaker: "M5.4 5.4a2 2 0 0 1 2-2h9.2a2 2 0 0 1 2 2v13.2a2 2 0 0 1-2 2H7.4a2 2 0 0 1-2-2Z|M12 10.6a3.4 3.4 0 1 0 0 6.8 3.4 3.4 0 0 0 0-6.8Z|d:12 6.8",
  watch: "M6.6 12a5.4 5.4 0 1 0 10.8 0 5.4 5.4 0 0 0-10.8 0Z|M9 7.2 9.4 3.4h5.2L15 7.2|M9 16.8l.4 3.8h5.2l.4-3.8|M12 9.6V12l1.8 1.2",
  directions_car: "M4.2 16.4v-3.1l1.8-4.5a2 2 0 0 1 1.9-1.3h8.2a2 2 0 0 1 1.9 1.3l1.8 4.5v3.1Z|M4.2 13.3h15.6|d:7.6 15.6|d:16.4 15.6|M5.4 16.4v2.2h2.6v-2.2|M16 16.4v2.2h2.6v-2.2",
  fitness_center: "M4 10.4v3.2|M6.8 7.8v8.4|M17.2 7.8v8.4|M20 10.4v3.2|M6.8 12h10.4",
  trending_up: "M4.2 16.2 9.2 11.2l3.4 3.4L19.8 7.4|M14.6 7.4h5.2v5.2",
  folder: "M3.6 7a2 2 0 0 1 2-2h3.6l2.1 2.4h7.1a2 2 0 0 1 2 2v7.6a2 2 0 0 1-2 2H5.6a2 2 0 0 1-2-2Z",
  folder_off: "M3.6 7a2 2 0 0 1 2-2h3.6l2.1 2.4h7.1a2 2 0 0 1 2 2v7.6a2 2 0 0 1-2 2H5.6a2 2 0 0 1-2-2Z|M4.4 4.4l15.2 15.2",
  archive: `${ARCHIVE_BOX}|M12 11.6v5.2|M9.6 14.4 12 16.8l2.4-2.4`,
  unarchive: `${ARCHIVE_BOX}|M12 16.8v-5.2|M9.6 14 12 11.6l2.4 2.4`,
  image: "M3.8 6.6a2.2 2.2 0 0 1 2.2-2.2h12a2.2 2.2 0 0 1 2.2 2.2v10.8a2.2 2.2 0 0 1-2.2 2.2H6a2.2 2.2 0 0 1-2.2-2.2Z|d:8.6 9.2|M4.4 16.8 9.2 12l3.4 3.4 3-3 4.2 4.2",
  description: "M6.8 3.8h6.4L18.4 9v10.8a1.6 1.6 0 0 1-1.6 1.6H6.8a1.6 1.6 0 0 1-1.6-1.6V5.4a1.6 1.6 0 0 1 1.6-1.6Z|M13.2 3.8V9h5.2|M8.4 13.2h7.2|M8.4 16.6h4.6",
  music_note: "M9.8 17.2V7.2l8.4-2.2v10|M7 14.4a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 0 0 0-5.6Z|M15.4 12.2a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 0 0 0-5.6Z",
  graphic_eq: "M4.4 10v4|M8.2 6.6v10.8|M12 3.8v16.4|M15.8 6.6v10.8|M19.6 10v4",
  download: "M12 4v10.4|M7.8 10.4 12 14.6l4.2-4.2|M5 19.4h14",
  downloading: "M12 4v8.4|M8.6 9.4 12 12.8l3.4-3.4|M5.4 15.6a7.4 7.4 0 0 0 13.2 0|M19.4 19.6H4.6",
  share: "M17.4 3.4a2.6 2.6 0 1 0 0 5.2 2.6 2.6 0 0 0 0-5.2Z|M6.6 9.4a2.6 2.6 0 1 0 0 5.2 2.6 2.6 0 0 0 0-5.2Z|M17.4 15.4a2.6 2.6 0 1 0 0 5.2 2.6 2.6 0 0 0 0-5.2Z|M8.9 10.7 15.1 7.3|M8.9 13.3 15.1 16.7",
  content_copy: "M9 8.6a2 2 0 0 1 2-2h7.4a2 2 0 0 1 2 2V17a2 2 0 0 1-2 2H11a2 2 0 0 1-2-2Z|M15.4 5.4V4.8a1.4 1.4 0 0 0-1.4-1.4H5a1.4 1.4 0 0 0-1.4 1.4v9.2A1.4 1.4 0 0 0 5 15.4h.6",
  history: "M4.4 12a7.6 7.6 0 1 0 2.3-5.4|M4 6.2v4.6h4.6|M12 7.8v4.4l3.1 1.8",
  translate: "M3.6 6.4h8.8|M8 4.2v2.2|M10.4 6.4c0 4.2-3.4 8.2-6.8 9.8|M5.8 10.8c1.4 3 3.8 4.8 6 5.6|M12.6 20.4l4.3-10.2 4.3 10.2|M14.3 16.6h5.2",
  visibility: "M2.6 12S6.4 6.2 12 6.2 21.4 12 21.4 12 17.6 17.8 12 17.8 2.6 12 2.6 12Z|M12 9.2a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 0 0 0-5.6Z",
  visibility_off: "M2.6 12S6.4 6.2 12 6.2 21.4 12 21.4 12 17.6 17.8 12 17.8 2.6 12 2.6 12Z|M4.4 4.4l15.2 15.2",
  dark_mode: "M19.8 14.6A8.4 8.4 0 0 1 9.4 4.2a8.4 8.4 0 1 0 10.4 10.4Z",
  calendar_today: "M3.8 7.4a2.2 2.2 0 0 1 2.2-2.2h12a2.2 2.2 0 0 1 2.2 2.2v10.4a2.2 2.2 0 0 1-2.2 2.2H6a2.2 2.2 0 0 1-2.2-2.2Z|M3.8 10h16.4|M8.4 3.4v3.6|M15.6 3.4v3.6",
  alarm: "M12 6a7 7 0 1 0 0 14 7 7 0 0 0 0-14Z|M12 9.4V13l2.6 1.8|M4.6 5.6 7.6 3|M19.4 5.6 16.4 3",
  schedule: "M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z|M12 7.4V12l3.2 1.9",
  timer: "M12 6.6a7 7 0 1 0 0 14 7 7 0 0 0 0-14Z|M9.4 3.4h5.2|M12 9.6v4",
  camera: "M3.8 8.6a2.1 2.1 0 0 1 2.1-2.1h2.3l1.3-2h5l1.3 2h2.3a2.1 2.1 0 0 1 2.1 2.1v8.3a2.1 2.1 0 0 1-2.1 2.1H5.9a2.1 2.1 0 0 1-2.1-2.1Z|M12 9.2a3.4 3.4 0 1 0 0 6.8 3.4 3.4 0 0 0 0-6.8Z",
  wifi: "M2.6 9.4a14 14 0 0 1 18.8 0|M6 12.8a9 9 0 0 1 12 0|d:12 17.4",
  lock: "M5 12.6a2.2 2.2 0 0 1 2.2-2.2h9.6a2.2 2.2 0 0 1 2.2 2.2v5a2.2 2.2 0 0 1-2.2 2.2H7.2A2.2 2.2 0 0 1 5 17.6Z|M8.6 10.4V8.2a3.4 3.4 0 0 1 6.8 0v2.2|d:12 15.1",
  notifications: "M6.6 16.4V11a5.4 5.4 0 1 1 10.8 0v5.4l1.4 2.2H5.2Z|M10 19.6a2 2 0 0 0 4 0",
  apps: "M5.2 4.6h3.4v3.4H5.2Z|M10.3 4.6h3.4v3.4h-3.4Z|M15.4 4.6h3.4v3.4h-3.4Z|M5.2 10.3h3.4v3.4H5.2Z|M10.3 10.3h3.4v3.4h-3.4Z|M15.4 10.3h3.4v3.4h-3.4Z|M5.2 16h3.4v3.4H5.2Z|M10.3 16h3.4v3.4h-3.4Z|M15.4 16h3.4v3.4h-3.4Z",
  swap_horiz: "M4.2 9.2h13.2|M14.4 6.2 17.4 9.2l-3 3|M19.8 14.8H6.6|M9.6 11.8l-3 3 3 3",
  arrow_upward: "M12 19.4V5.2|M6.6 10.6 12 5.2l5.4 5.4",
  arrow_downward: "M12 4.6v14.2|M6.6 13.4 12 18.8l5.4-5.4",
  person_add: "M10 4.6a3.6 3.6 0 1 0 0 7.2 3.6 3.6 0 0 0 0-7.2Z|M3.6 19.4a6.4 6.4 0 0 1 12.8 0|M18.6 13.4v5.2|M16 16h5.2",
  airplanemode_active: "M12 3.4a1.3 1.3 0 0 1 1.3 1.3v4.6l7.1 4.1v2.2l-7.1-2.2v4l2.4 1.8v1.4L12 19.6l-3.7 1v-1.4l2.4-1.8v-4L3.6 15.4v-2.2l7.1-4.1V4.7A1.3 1.3 0 0 1 12 3.4Z",
  brightness_6: "M12 7.4a4.6 4.6 0 1 0 0 9.2 4.6 4.6 0 0 0 0-9.2Z|M12 2.6v2.2|M12 19.2v2.2|M2.6 12h2.2|M19.2 12h2.2|M5.3 5.3 6.9 6.9|M17.1 17.1l1.6 1.6|M18.7 5.3l-1.6 1.6|M6.9 17.1l-1.6 1.6",
  brightness_auto: "M12 6.8a5.2 5.2 0 1 0 0 10.4 5.2 5.2 0 0 0 0-10.4Z|M12 2.6v1.8|M12 19.6v1.8|M2.6 12h1.8|M19.6 12h1.8|M5.3 5.3 6.6 6.6|M17.4 17.4l1.3 1.3|M18.7 5.3l-1.3 1.3|M6.6 17.4l-1.3 1.3|M10.2 14.4 12 9.6l1.8 4.8|M10.8 12.9h2.4",
  format_size: "M3.2 17.4 7.4 6.6l4.2 10.8|M4.6 14.2h5.6|M14.4 17.4 17.2 10.4l2.8 7|M15.4 15.4h3.6",
  badge: "M3.8 7.4a2 2 0 0 1 2-2h12.4a2 2 0 0 1 2 2v9.2a2 2 0 0 1-2 2H5.8a2 2 0 0 1-2-2Z|M9.4 3.6h5.2v3.8H9.4Z|M9 11.2a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6Z|M14.2 11.8h3.6|M14.2 15h3.6",
  devices: "M3.2 6.4a1.6 1.6 0 0 1 1.6-1.6h10.8a1.6 1.6 0 0 1 1.6 1.6v1.4|M3.2 8.6v6.2a1.6 1.6 0 0 0 1.6 1.6h6.6|M6 19.4h4|M15.2 10.4a1.4 1.4 0 0 1 1.4-1.4h3.4a1.4 1.4 0 0 1 1.4 1.4v7.6a1.4 1.4 0 0 1-1.4 1.4h-3.4a1.4 1.4 0 0 1-1.4-1.4Z",
  laptop_mac: "M5.4 6.4a1.6 1.6 0 0 1 1.6-1.6h10a1.6 1.6 0 0 1 1.6 1.6v8.2H5.4Z|M2.8 17.6h18.4|M10.4 17.6h3.2",
  smartphone: "M7 4.6a1.8 1.8 0 0 1 1.8-1.8h6.4A1.8 1.8 0 0 1 17 4.6v14.8a1.8 1.8 0 0 1-1.8 1.8H8.8A1.8 1.8 0 0 1 7 19.4Z|M10.4 5.8h3.2|d:12 18.2",
  keyboard: "M2.8 7.6a1.6 1.6 0 0 1 1.6-1.6h15.2a1.6 1.6 0 0 1 1.6 1.6v8.8a1.6 1.6 0 0 1-1.6 1.6H4.4a1.6 1.6 0 0 1-1.6-1.6Z|M8 14.8h8|d:6 9.6|d:9.4 9.6|d:12.8 9.6|d:16.2 9.6|d:18 12.4|d:6 12.4",
  public: "M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z|M4 12h16|M12 4c2.2 2.2 3.4 5 3.4 8s-1.2 5.8-3.4 8c-2.2-2.2-3.4-5-3.4-8S9.8 6.2 12 4Z",
  language: "M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z|M4.4 9.4h15.2|M4.4 14.6h15.2|M12 4c2 2.4 3 5 3 8s-1 5.6-3 8c-2-2.4-3-5-3-8s1-5.6 3-8Z",
  hourglass_empty: "M7 3.6h10|M7 20.4h10|M7.6 3.6v3.2L12 12l-4.4 5.2v3.2|M16.4 3.6v3.2L12 12l4.4 5.2v3.2",
  palette: "M12 4a8 8 0 0 0 0 16 1.8 1.8 0 0 0 1.8-1.8c0-.5-.2-.9-.5-1.2a1.8 1.8 0 0 1 1.3-3h1.9A4.5 4.5 0 0 0 21 9.5C21 6.4 16.9 4 12 4Z|d:7.6 12|d:9.4 8.2|d:14.2 7.8|d:17.2 10.6",
  animation: "M9 8.4a5.6 5.6 0 1 0 0 11.2 5.6 5.6 0 0 0 0-11.2Z|M11.6 6.2h4.2a2 2 0 0 1 2 2v4.2|M14.2 3.8h4a2 2 0 0 1 2 2v4",
  mark_email_read: "M3.6 7.6a1.8 1.8 0 0 1 1.8-1.8h10.2a1.8 1.8 0 0 1 1.8 1.8v3|M3.6 8.2 10.5 13l3.3-2.3|M3.6 8.2v8a1.8 1.8 0 0 0 1.8 1.8h6.2|M14.4 17.2l2 2 4.2-4.6",
  devices_other: "M3.2 6.4a1.6 1.6 0 0 1 1.6-1.6h10.8a1.6 1.6 0 0 1 1.6 1.6v1.4|M3.2 8.6v6.2a1.6 1.6 0 0 0 1.6 1.6h6.6|M6 19.4h4|M15.2 10.4a1.4 1.4 0 0 1 1.4-1.4h3.4a1.4 1.4 0 0 1 1.4 1.4v7.6a1.4 1.4 0 0 1-1.4 1.4h-3.4a1.4 1.4 0 0 1-1.4-1.4Z",
  send: "M20.4 4 3.6 12l16.8 8-3.2-8Z|M17.2 12H6.4",
  error: "M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z|M12 7.8v5.4|d:12 16.4",

  /* general & actions */
  remove: "M4.8 12h14.4",
  save: "M5 4.4h11l3.6 3.6v11.6H5Z|M8.4 4.4v4.4h6.4V4.4|M8.4 19.6v-5.4h7.2v5.4",
  cancel: `${C8}|M9 9l6 6|M15 9l-6 6`,
  block: `${C8}|M6.4 6.4l11.2 11.2`,
  tune: "M4 7h8|M16 7h4|M4 17h4|M12 17h8|M14 4.6v4.8|M10 14.6v4.8",
  sort: "M4 7h16|M4 12h11|M4 17h6",
  view_list: "M8.4 6.6h11.2|M8.4 12h11.2|M8.4 17.4h11.2|d:4.8 6.6|d:4.8 12|d:4.8 17.4",
  grid_view: "M4.6 4.6h5.8v5.8H4.6Z|M13.6 4.6h5.8v5.8h-5.8Z|M4.6 13.6h5.8v5.8H4.6Z|M13.6 13.6h5.8v5.8h-5.8Z",
  dashboard: "M4.4 4.4h6.4v8.2H4.4Z|M13.2 4.4h6.4v4.4h-6.4Z|M13.2 11.2h6.4v8.4h-6.4Z|M4.4 15h6.4v4.6H4.4Z",
  push_pin: "M9 3.8h6|M10 3.8v5.4l-3.2 3.6v1.6h10.4v-1.6L14 9.2V3.8|M12 14.4v5.8",
  pin: "M3.6 7.6a1.8 1.8 0 0 1 1.8-1.8h13.2a1.8 1.8 0 0 1 1.8 1.8v8.8a1.8 1.8 0 0 1-1.8 1.8H5.4a1.8 1.8 0 0 1-1.8-1.8Z|d:8 12|d:12 12|d:16 12",
  attach_file: "M16.4 7.4v8.4a4.4 4.4 0 0 1-8.8 0V6.6a2.8 2.8 0 0 1 5.6 0v8.8a1.2 1.2 0 0 1-2.4 0V7.8",
  content_paste: "M6.6 5.4H6a1.6 1.6 0 0 0-1.6 1.6v12.2A1.6 1.6 0 0 0 6 20.8h12a1.6 1.6 0 0 0 1.6-1.6V7a1.6 1.6 0 0 0-1.6-1.6h-.6|M8.8 3.4h6.4v3.6H8.8Z",
  code: "M8.6 7 3.6 12l5 5|M15.4 7l5 5-5 5",
  terminal: "M3.6 7a2 2 0 0 1 2-2h12.8a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5.6a2 2 0 0 1-2-2Z|M7.4 9.4l2.8 2.6-2.8 2.6|M12.4 15h4.2",
  lock_open: "M5 12.6a2.2 2.2 0 0 1 2.2-2.2h9.6a2.2 2.2 0 0 1 2.2 2.2v5a2.2 2.2 0 0 1-2.2 2.2H7.2A2.2 2.2 0 0 1 5 17.6Z|M8.6 10.4V8.2a3.4 3.4 0 0 1 6.6-1.2|d:12 15.1",
  security: SHIELD,
  privacy_tip: `${SHIELD}|M12 11v4.4|d:12 8.2`,
  build: "M14.6 4.2a4.2 4.2 0 0 0-4.4 5.6l-5.8 5.8a1.8 1.8 0 0 0 2.6 2.6l5.8-5.8a4.2 4.2 0 0 0 5.6-4.4l-2.6 2.6-2.4-.4-.4-2.4Z",
  handyman: "M5 19l8.4-8.4|M10.4 5.6 13.6 3l5.4 5.4-2.6 2.6-2.2-.6-1-1-.6-2.2Z",
  widgets: "M4.4 4.4h6v6h-6Z|M4.4 13.6h6v6h-6Z|M13.6 13.6h6v6h-6Z|M16.6 3.4l3.9 3.9-3.9 3.9-3.9-3.9Z",
  wallpaper: "M4 9V6a2 2 0 0 1 2-2h3|M15 4h3a2 2 0 0 1 2 2v3|M20 15v3a2 2 0 0 1-2 2h-3|M9 20H6a2 2 0 0 1-2-2v-3|M7.4 16.4l3-3.6 2.2 2.4 1.8-2 2.2 3.2Z|d:15.4 8.6",
  favorite: "M12 19.4s-7.6-4.6-7.6-10a4.2 4.2 0 0 1 7.6-2.5 4.2 4.2 0 0 1 7.6 2.5c0 5.4-7.6 10-7.6 10Z",
  location_on: "M12 20.8s-6.2-5.6-6.2-10.6a6.2 6.2 0 0 1 12.4 0c0 5-6.2 10.6-6.2 10.6Z|M12 7.8a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 0 0 0-4.8Z",
  my_location: "M12 5.6a6.4 6.4 0 1 0 0 12.8 6.4 6.4 0 0 0 0-12.8Z|M12 2.8v2.8|M12 18.4v2.8|M2.8 12h2.8|M18.4 12h2.8|d:12 12",
  navigation: "M12 3.6 18.6 19.6 12 16.2 5.4 19.6Z",
  explore: `${C8}|M15.4 8.6l-2 4.8-4.8 2 2-4.8Z`,
  route: "M6.4 3.8a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4Z|M17.6 15.8a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4Z|M8.6 6h7.6a2.8 2.8 0 0 1 0 5.6H7.8a2.8 2.8 0 0 0 0 5.6h7.6",
  work: "M3.8 9a2 2 0 0 1 2-2h12.4a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5.8a2 2 0 0 1-2-2Z|M9 7V5.4A1.4 1.4 0 0 1 10.4 4h3.2A1.4 1.4 0 0 1 15 5.4V7|M3.8 12.6h16.4",

  /* system */
  power_settings_new: "M12 3.6v7.6|M7.4 6.4a7 7 0 1 0 9.2 0",
  battery_full: `${BATT}|M7 9.4h8a1 1 0 0 1 1 1v3.2a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1v-3.2a1 1 0 0 1 1-1Z`,
  battery_charging_full: `${BATT}|M11.8 9 9.6 12.2h3.4l-2 2.8`,
  battery_saver: `${BATT}|M10.9 9.8v4.4|M8.7 12h4.4`,
  signal_cellular_alt: "M5.4 19v-4|M10 19v-7.4|M14.6 19V8|M19.2 19V4.6",
  sim_card: "M7.8 3.6h6.8l3.6 3.6v12a1.6 1.6 0 0 1-1.6 1.6H7.8a1.6 1.6 0 0 1-1.6-1.6V5.2a1.6 1.6 0 0 1 1.6-1.6Z|M9 11h6v6.4H9Z|M12 11v6.4",
  vibration: "M8.6 5h6.8v14H8.6Z|M5.4 8.4v7.2|M18.6 8.4v7.2|M2.8 10.2v3.6|M21.2 10.2v3.6",
  do_not_disturb_on: `${C8}|M8 12h8`,
  notifications_off: `${BELL}|${SLASH}`,
  nightlight: "M15.6 4.4a8 8 0 1 0 0 15.2 8 8 0 0 1 0-15.2Z",
  screen_rotation: "M9.4 4.2l10.4 10.4-5.2 5.2L4.2 9.4Z|M3.4 15.4a8.6 8.6 0 0 0 5.2 5.2|M20.6 8.6a8.6 8.6 0 0 0-5.2-5.2",
  storage: "M4 5.4h16V9H4Z|M4 10.2h16v3.6H4Z|M4 15h16v3.6H4Z|d:6.8 7.2|d:6.8 12|d:6.8 16.8",
  memory: "M7 7h10v10H7Z|M10 10h4v4h-4Z|M9.4 4v3|M14.6 4v3|M9.4 17v3|M14.6 17v3|M4 9.4h3|M4 14.6h3|M17 9.4h3|M17 14.6h3",
  speed: "M4.8 17.6a8 8 0 1 1 14.4 0|M12 14.2l4-5|d:12 14.2",
  sensors: "d:12 12|M8.6 8.6a4.8 4.8 0 0 0 0 6.8|M15.4 8.6a4.8 4.8 0 0 1 0 6.8|M5.8 5.8a8.8 8.8 0 0 0 0 12.4|M18.2 5.8a8.8 8.8 0 0 1 0 12.4",
  accessibility: "d:12 4.6|M5 8.4l7 1.4 7-1.4|M12 9.8V14|M9 20l3-6 3 6",
  autorenew: "M18.6 9.4A7 7 0 0 0 5.4 10|M18.6 4.8v4.6H14|M5.4 14.6a7 7 0 0 0 13.2.6|M5.4 19.2v-4.6H10",
  sos: "M7.2 8.6H4.4a1.6 1.6 0 0 0 0 3.2h1.2a1.6 1.6 0 0 1 0 3.2H2.8|M12 8.6a3.2 3.2 0 1 0 0 6.4 3.2 3.2 0 0 0 0-6.4Z|M21.2 8.6h-2.8a1.6 1.6 0 0 0 0 3.2h1.2a1.6 1.6 0 0 1 0 3.2h-2.8",

  /* media */
  skip_next: "M6 6l8.4 6L6 18Z|M18 6v12",
  skip_previous: "M18 6l-8.4 6L18 18Z|M6 6v12",
  stop: "M6.6 6.6h10.8v10.8H6.6Z",
  stop_circle: `${C8}|M9.4 9.4h5.2v5.2H9.4Z`,
  play_circle: `${C8}|M10 8.6 15.4 12 10 15.4Z`,
  repeat: REPEAT,
  repeat_one: `${REPEAT}|M11.4 10.6l1.2-.8v4.6`,
  shuffle: "M4 7h3.4l9.2 10H20|M4 17h3.4l2.4-2.6|M14.2 9.6 16.6 7H20|M17.6 4l2.8 3-2.8 3|M17.6 14l2.8 3-2.8 3",
  equalizer: "M6 19.4v-8|M10 19.4V4.6|M14 19.4v-11|M18 19.4v-5",
  queue_music: "M4 6.4h11|M4 11h11|M4 15.6h6|M18.6 6v10.6|M18.6 6H21|M16.4 14.4a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4Z",
  playlist_add: "M4 6.4h11|M4 11h11|M4 15.6h7|M17.6 12.4v7.2|M14 16h7.2",
  album: `${C8}|M12 9.8a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4Z`,
  volume_down: "M5.4 9.4h3.4l4.6-3.8v12.8l-4.6-3.8H5.4Z|M16.7 9.4a3.7 3.7 0 0 1 0 5.2",
  videocam: "M3.4 7.6a1.8 1.8 0 0 1 1.8-1.8h8.6a1.8 1.8 0 0 1 1.8 1.8v8.8a1.8 1.8 0 0 1-1.8 1.8H5.2a1.8 1.8 0 0 1-1.8-1.8Z|M15.6 10.4l5-3v9.2l-5-3",
  movie: "M3.8 6a1.6 1.6 0 0 1 1.6-1.6h13.2A1.6 1.6 0 0 1 20.2 6v12a1.6 1.6 0 0 1-1.6 1.6H5.4A1.6 1.6 0 0 1 3.8 18Z|M3.8 9h16.4|M8 4.4 9.6 9|M13 4.4 14.6 9",
  record_voice_over: "M9.4 5a3.4 3.4 0 1 0 0 6.8 3.4 3.4 0 0 0 0-6.8Z|M3.4 19.6a6 6 0 0 1 12 0|M16.4 6.4a3.6 3.6 0 0 1 0 4|M19 4a7.2 7.2 0 0 1 0 8.8",

  /* camera & gallery */
  flash_on: FLASH,
  flash_off: `${FLASH}|${SLASH}`,
  flash_auto: "M10.4 3.4 4.4 12h4.4L8 19.6 14 11H9.6Z|M14.6 19.6l2.6-7 2.6 7|M15.4 17.4H19",
  flashlight_on: `${TORCH}|d:12 13.6`,
  flashlight_off: `${TORCH}|${SLASH}`,
  grid_on: "M4.4 4.4h15.2v15.2H4.4Z|M9.5 4.4v15.2|M14.5 4.4v15.2|M4.4 9.5h15.2|M4.4 14.5h15.2",
  high_quality: `${RECT}|M6.6 9v6|M10 9v6|M6.6 12H10|M13.4 9h3.4v6h-3.4Z|M16 14l1.4 1.6`,
  aspect_ratio: `${RECT}|M6.6 11V8.4h3|M17.4 13v2.6h-3`,
  crop: "M6.6 3.4v12.2a1.8 1.8 0 0 0 1.8 1.8h12.2|M3.4 6.6h12.2a1.8 1.8 0 0 1 1.8 1.8v12.2",
  rotate_left: "M5.6 12.6a6.8 6.8 0 1 0 2-4.8|M4.2 4.4V9h4.6",
  flip: "M12 3.4v17.2|M9 6.4 3.6 17.6H9Z|M15 6.4l5.4 11.2H15Z",
  auto_awesome: "M10 4.6l1.6 4.4 4.4 1.6-4.4 1.6-1.6 4.4-1.6-4.4L4 10.6l4.4-1.6Z|M17.6 13.6l.8 2.2 2.2.8-2.2.8-.8 2.2-.8-2.2-2.2-.8 2.2-.8Z|M17.4 3.6v3.2|M15.8 5.2H19",
  filter_vintage: "M12 9.6a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 0 0 0-4.8Z|M12 9.6c-2-3-1.4-5.4 0-6.2 1.4.8 2 3.2 0 6.2Z|M12 14.4c2 3 1.4 5.4 0 6.2-1.4-.8-2-3.2 0-6.2Z|M9.6 12c-3 2-5.4 1.4-6.2 0 .8-1.4 3.2-2 6.2 0Z|M14.4 12c3-2 5.4-1.4 6.2 0-.8 1.4-3.2 2-6.2 0Z",
  no_photography: `${CAM}|${SLASH}`,
  add_a_photo: `${CAM}|M19.4 1.8v3.6|M17.6 3.6h3.6`,

  /* files */
  create_new_folder: `${FOLDER}|M12 10.4v5.2|M9.4 13h5.2`,
  drive_file_move: `${FOLDER}|M8.4 13h6.4|M12.4 10.4 15 13l-2.6 2.6`,
  drive_file_rename_outline: "M4.4 19.6h3L17.8 9.2l-3-3L4.4 16.6Z|M13 8l3 3|M12 19.6h7.6",
  picture_as_pdf: `${DOC}|M8.4 13v4.4|M8.4 13h1.4a1.2 1.2 0 0 1 0 2.4H8.4|M12.2 13v4.4h.8a1.6 1.6 0 0 0 1.6-1.6v-1.2a1.6 1.6 0 0 0-1.6-1.6Z`,
  download_done: "M5.6 11.4l4.4 4.4 8.4-8.8|M5 19.4h14",
  android: "M4.6 17.4a7.4 7.4 0 0 1 14.8 0Z|M8 8.4 6.4 5.8|M16 8.4l1.6-2.6|d:9.2 13.8|d:14.8 13.8",

  /* books & text */
  book: "M6 4.4h11.6v15.2H7.4A1.4 1.4 0 0 1 6 18.2Z|M6 17.4A1.4 1.4 0 0 1 7.4 16h10.2|M10 4.4v6l1.8-1.2 1.8 1.2v-6",
  menu_book: "M12 6.8c-2-1.6-4.8-2-8-1.6v12.6c3.2-.4 6 0 8 1.6 2-1.6 4.8-2 8-1.6V5.2c-3.2-.4-6 0-8 1.6Z|M12 6.8v12.6",
  library_books: "M7.6 3.8h11a1.6 1.6 0 0 1 1.6 1.6v11a1.6 1.6 0 0 1-1.6 1.6h-11A1.6 1.6 0 0 1 6 16.4v-11a1.6 1.6 0 0 1 1.6-1.6Z|M3.4 7.4v11.4a1.8 1.8 0 0 0 1.8 1.8h11.4|M9.6 8H16|M9.6 11H16|M9.6 14h4",
  book_shelf: "M3 20h18|M4.6 4.6h3v15.4h-3Z|M8.6 6.6h3v13.4h-3Z|M12.8 5.6l2.8-.8 3.6 14.6-2.8.8Z|M4.6 8h3|M8.6 9.6h3",
  book_open: "M12 18.6c-2.4-1.6-5.4-1.8-8.6-1.2V6.6c3.2-.6 6.2-.4 8.6 1.2 2.4-1.6 5.4-1.8 8.6-1.2v10.8c-3.2-.6-6.2-.4-8.6 1.2Z|M12 7.8v10.8|M3.4 19.8c3.2-.6 6.2-.2 8.6 1.2 2.4-1.4 5.4-1.8 8.6-1.2",
  auto_stories: "M3.6 6.2 11 8.6v11.2l-7.4-2.4Z|M11 8.6l6.6-4.8v11.4L11 19.8|M20.4 7v11.6",
  text_increase: `${TXT_A}|M18 9v6`,
  text_decrease: TXT_A,
  spellcheck: "M3.4 13.4 6.8 4.6l3.4 8.8|M4.6 10.4H9|M11.8 15.2l3.2 3.2 6-6.4",

  /* weather */
  wb_sunny: "M12 7.6a4.4 4.4 0 1 0 0 8.8 4.4 4.4 0 0 0 0-8.8Z|M12 2.6v2|M12 19.4v2|M2.6 12h2|M19.4 12h2|M5.4 5.4l1.4 1.4|M17.2 17.2l1.4 1.4|M18.6 5.4l-1.4 1.4|M6.8 17.2l-1.4 1.4",
  wb_cloudy: "M8.6 4.6a3.6 3.6 0 0 0-2.8 5.9|M8.6 2.2V3|M3 8.2h.8|M4.6 4.2l.6.6|M8.6 19.4a3.6 3.6 0 0 1-.4-7.2 5 5 0 0 1 9.6 1.1 3 3 0 0 1-.2 6.1Z",
  wb_twilight: "M5.6 16.4a6.4 6.4 0 0 1 12.8 0|M3 19.4h18|M12 4.6v2.6|M4.6 8.6l1.8 1.8|M19.4 8.6l-1.8 1.8",
  cloud: "M7.2 18.4a4 4 0 0 1-.4-8 5.6 5.6 0 0 1 10.8 1.2 3.4 3.4 0 0 1-.4 6.8Z",
  umbrella: "M3.4 12a8.6 8.6 0 0 1 17.2 0Z|M12 12v6.2a2 2 0 0 1-4 0|M12 2.6v.8",
  ac_unit: "M12 3v18|M4.2 7.5l15.6 9|M4.2 16.5l15.6-9|M9.8 4.4 12 6.4l2.2-2|M9.8 19.6l2.2-2 2.2 2",
  thermostat: "M9.8 5.6a2.2 2.2 0 0 1 4.4 0v8.2a4 4 0 1 1-4.4 0Z|M12 9.4v7",
  grain: "d:6 6|d:12 8|d:9 12.4|d:15.4 12|d:6 17|d:18 6|d:12 17.6|d:18 17",
  dehaze: "M4 7h16|M4 12h16|M4 17h16",

  /* fitness */
  directions_run: "M15.2 3.4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2Z|M6.4 9.6l3.4-2.2h3.6l2 3.4 3 1|M12.6 7.4 10 13.4l3.6 2.4-1 4.6|M10 13.4 8 17H4.4",
  directions_walk: "M13.2 3.4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2Z|M12.4 8.4 10.6 14l2.8 2.6.6 4|M10.6 14l-1.8 6.4|M12.4 8.4l-3.4 2v3|M12.4 8.4l1.6 3 2.6 1",
  directions_bike: "M6 12.6a3.6 3.6 0 1 0 0 7.2 3.6 3.6 0 0 0 0-7.2Z|M18 12.6a3.6 3.6 0 1 0 0 7.2 3.6 3.6 0 0 0 0-7.2Z|M6 16.2 10 10l3 3.6h5|M10 10h4.4|M15.6 3.6a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2Z|M18 16.2l-3.2-6.8",
  pool: "M3 18c1.5-1.2 3-1.2 4.5 0s3 1.2 4.5 0 3-1.2 4.5 0 3 1.2 4.5 0|M7.6 14.2 13 8.6l-2.6-2.4-3 .8|M13 8.6l4 5.6|M17 5.4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2Z",
  directions_boat: "M3.6 15.2 12 12.4l8.4 2.8-2 4.4H5.6Z|M6.4 14.2V8.6h11.2v5.6|M12 5v3.6|M9.6 5h4.8|M12 12.4v7.2",
  terrain: "M2.8 18.6 9 8.6l3.6 5.6 2.4-3.4 6.2 7.8Z",
  spa: "M12 19.4c-4.6 0-8-2.8-8.4-7 3.6 0 6.8 2 8.4 7Z|M12 19.4c4.6 0 8-2.8 8.4-7-3.6 0-6.8 2-8.4 7Z|M12 19.4c-2.4-2.6-2.4-9.6 0-14 2.4 4.4 2.4 11.4 0 14Z",
  whatshot: FLAME,
  local_fire_department: `${FLAME}|M12 20.4a2.6 2.6 0 0 1-2.6-2.6c0-1.6 1.4-2.4 2.6-4.2 1.2 1.8 2.6 2.6 2.6 4.2a2.6 2.6 0 0 1-2.6 2.6Z`,
  emoji_events: "M7.4 4.2h9.2v5.4a4.6 4.6 0 0 1-9.2 0Z|M7.4 6H4.6v1.6a3 3 0 0 0 3 3|M16.6 6h2.8v1.6a3 3 0 0 1-3 3|M12 14.2v3.2|M9.6 17.4h4.8V20H9.6Z",
  military_tech: "M7.6 3.4l2.8 6.4|M16.4 3.4l-2.8 6.4|M7.6 3.4h8.8|M12 9.8a4.8 4.8 0 1 0 0 9.6 4.8 4.8 0 0 0 0-9.6Z|M12 12.2l.9 1.8 2 .3-1.4 1.4.3 2-1.8-.9-1.8.9.3-2-1.4-1.4 2-.3Z",
  videogame_asset: "M3.4 9a2 2 0 0 1 2-2h13.2a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H5.4a2 2 0 0 1-2-2Z|M8 10v4|M6 12h4|d:15.2 11|d:17.4 13.2",

  /* tools */
  calculate: "M5 5.4a1.6 1.6 0 0 1 1.6-1.6h10.8A1.6 1.6 0 0 1 19 5.4v13.2a1.6 1.6 0 0 1-1.6 1.6H6.6A1.6 1.6 0 0 1 5 18.6Z|M8 7.4h8v3H8Z|d:8.6 13.6|d:12 13.6|d:15.4 13.6|d:8.6 17|d:12 17|d:15.4 17",
  functions: "M17.4 4.6H6.6l6 7.4-6 7.4h10.8",
  percent: "M18.4 5.6 5.6 18.4|M7.6 5a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 0 0 0-4.8Z|M16.4 14.2a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 0 0 0-4.8Z",
  numbers: "M9.6 4 7.6 20|M16.4 4l-2 16|M4.6 9h15.2|M3.8 15H19",
  straighten: "M2.8 8.4h18.4v7.2H2.8Z|M6.4 8.4v3|M10 8.4v4.2|M13.6 8.4v3|M17.2 8.4v4.2",
  architecture: "M12 3.4a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6Z|M11.2 6.8 5.6 20.4|M12.8 6.8l5.6 13.6|M7.2 16.2a9 9 0 0 0 9.6 0",
  swap_vert: "M8.6 18.8V5.6|M5.4 8.6l3.2-3.2 3.2 3.2|M15.4 5.2v13.2|M12.2 15.4l3.2 3.2 3.2-3.2",
  qr_code_scanner: `${CORNERS}|M7.6 7.6h3.2v3.2H7.6Z|M13.2 7.6h3.2v3.2h-3.2Z|M7.6 13.2h3.2v3.2H7.6Z|M13.2 13.2h3.2v3.2h-3.2Z`,
  document_scanner: `${CORNERS}|M3.6 12h16.8|M8 8h8|M8 16h5`,
  receipt: "M5.6 3.6v17l2.1-1.4 2.1 1.4 2.2-1.4 2.2 1.4 2.1-1.4 2.1 1.4v-17l-2.1 1.4-2.1-1.4-2.2 1.4-2.2-1.4-2.1 1.4Z|M8.8 9h6.4|M8.8 12.4h6.4|M8.8 15.8h4",
  casino: "M4.4 6.4a2 2 0 0 1 2-2h11.2a2 2 0 0 1 2 2v11.2a2 2 0 0 1-2 2H6.4a2 2 0 0 1-2-2Z|d:8.4 8.4|d:15.6 8.4|d:12 12|d:8.4 15.6|d:15.6 15.6",
  vpn_key: "M7.4 8.2a3.8 3.8 0 1 0 0 7.6 3.8 3.8 0 0 0 0-7.6Z|M11.2 12h9.4v3|M17.4 12v2.4",
  local_cafe: "M4.6 8.4h11.2v6.4a4 4 0 0 1-4 4H8.6a4 4 0 0 1-4-4Z|M15.8 9.6h1.8a2.2 2.2 0 0 1 0 4.4h-1.8|M3.6 21h14|M8 3.4v2.4|M11.6 3.4v2.4",
  checklist: "M3.6 6.6l1.6 1.6L8 5.2|M3.6 14.6l1.6 1.6L8 13.2|M11.4 6.8h9|M11.4 14.8h9",
  radio_button_checked: `${C8}|M12 8.6a3.4 3.4 0 1 0 0 6.8 3.4 3.4 0 0 0 0-6.8Z`,
  radio_button_unchecked: C8,

  /* misc */
  email: "M3.4 7a1.8 1.8 0 0 1 1.8-1.8h13.6A1.8 1.8 0 0 1 20.6 7v10a1.8 1.8 0 0 1-1.8 1.8H5.2A1.8 1.8 0 0 1 3.4 17Z|M3.8 7.2 12 13l8.2-5.8",
  business: "M3.6 20.2V4.6H12v15.6|M12 8.8h8.4v11.4|M2.6 20.2h18.8|M6.4 7.8h2.8|M6.4 11.2h2.8|M6.4 14.6h2.8|M15 12h2.4|M15 15.4h2.4",
  groups: "M12 5.4a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 0 0 0-5.6Z|M6.6 18.6a5.4 5.4 0 0 1 10.8 0|M5.6 8.2a2 2 0 1 0 0 4 2 2 0 0 0 0-4Z|M18.4 8.2a2 2 0 1 0 0 4 2 2 0 0 0 0-4Z|M2.4 17.4A3.6 3.6 0 0 1 6 14.2|M21.6 17.4a3.6 3.6 0 0 0-3.6-3.2",
  contact_phone: "M2.8 6.4a1.8 1.8 0 0 1 1.8-1.8h14.8a1.8 1.8 0 0 1 1.8 1.8v11.2a1.8 1.8 0 0 1-1.8 1.8H4.6a1.8 1.8 0 0 1-1.8-1.8Z|M8.6 8.4a2 2 0 1 0 0 4 2 2 0 0 0 0-4Z|M5.4 16.4a3.4 3.4 0 0 1 6.4 0|M14.4 9h3.8|M14.4 12h3.8|M14.4 15h2.4",
  face: `${C8}|d:9.2 10.6|d:14.8 10.6|M9 14.4a4 4 0 0 0 6 0`,
  mouse: "M6.4 9.4a5.6 5.6 0 0 1 11.2 0v5.2a5.6 5.6 0 0 1-11.2 0Z|M12 3.8v4.6",
  hub: "M12 9.6a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 0 0 0-4.8Z|M12 3.4a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6Z|M4.8 15.6a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6Z|M19.2 15.6a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6Z|M12 7v2.6|M10 13.4l-3.8 2.8|M14 13.4l3.8 2.8",
  bolt: "M13 3.4 6.6 13.2h5.2L11 20.6l6.4-9.8h-5.2Z",
  restaurant: "M6.4 3.6v5.6a2 2 0 0 0 4 0V3.6|M8.4 3.6v16.8|M17.4 20.4V3.6c-2.2 1-3.4 3.6-3.4 7.6h3.4",
  storefront: "M4.2 8.4l1.4-4h12.8l1.4 4v1a2.6 2.6 0 0 1-5.2 0 2.6 2.6 0 0 1-5.2 0 2.6 2.6 0 0 1-5.2 0Z|M5.4 12v7.6h13.2V12|M10 19.6v-4.4h4v4.4",
  sports_esports: "M7.4 6.8h9.2a4 4 0 0 1 3.9 3.2l1 5.2a2.4 2.4 0 0 1-4.2 2l-2-2.4H8.7l-2 2.4a2.4 2.4 0 0 1-4.2-2l1-5.2a4 4 0 0 1 3.9-3.2Z|M8 9.6v3.2|M6.4 11.2h3.2|d:15.6 10.2|d:17.4 12.2",
  celebration: "M4 20l4.6-12.4 7.8 7.8Z|M13.6 4.4c.8 1.6.4 3-1 4|M19.6 10.4c-1.6-.8-3-.4-4 1|d:17 5|d:20 7.4|d:10.4 3.6",
  cleaning_services: "M10.6 3.4h2.8v7.2h-2.8Z|M5.6 10.6h12.8l1.2 10H4.4Z|M9 20.6v-3.4|M12 20.6v-3.4|M15 20.6v-3.4",
  gavel: "M8.2 7.4l4.4-4.4 5.2 5.2-4.4 4.4Z|M10.6 10.4l-6 6a1.4 1.4 0 0 0 2 2l6-6|M12.4 20.4h8",
  health_and_safety: `${SHIELD}|M12 9v5.6|M9.2 11.8h5.6`,
  troubleshoot: "M10 3.8a6.2 6.2 0 1 0 0 12.4 6.2 6.2 0 0 0 0-12.4Z|M14.4 14.4l5.8 5.8|M6 10.4h1.8L9 8.2l1.8 4 1.2-1.8H14",
  settings_remote: "M8.4 7.6h7.2a1 1 0 0 1 1 1v11.8H7.4V8.6a1 1 0 0 1 1-1Z|M12 10.2a1.4 1.4 0 1 0 0 2.8 1.4 1.4 0 0 0 0-2.8Z|M9 4.6a4.4 4.4 0 0 1 6 0|M6.6 2.4a7.6 7.6 0 0 1 10.8 0",
  air: "M3.4 8.8h10.4a2.6 2.6 0 1 0-2.6-2.6|M3.4 12.4h15a2.6 2.6 0 1 1-2.6 2.6|M3.4 16h7",
  fiber_manual_record: "M12 6.4a5.6 5.6 0 1 0 0 11.2 5.6 5.6 0 0 0 0-11.2Z"
};

function FosIcon({ name, size = 24, color = "currentColor", opacity = 1, fill = 0, strokeWidth = 1.6, weight, style, ...rest }) {
  const spec = P[name];
  if (!spec) return /*#__PURE__*/React.createElement(__ds_scope.Icon, _extends({
  name: name,
  size: size,
  color: color,
  opacity: opacity,
  fill: fill,
  weight: weight,
  style: style
}, rest));
  const parts = spec.split("|");
  const solid = fill === 1 || fill === true;
  return /*#__PURE__*/React.createElement("svg", _extends({
  viewBox: "0 0 24 24",
  width: size,
  height: size,
  fill: "none",
  stroke: color,
  strokeWidth: strokeWidth,
  strokeLinecap: "round",
  strokeLinejoin: "round",
  "aria-hidden": "true",
  style: { opacity, flex: "0 0 auto", display: "block", ...style }
}, rest), parts.map((p, i) =>
        p.startsWith("d:")
          ? (() => { const [cx, cy] = p.slice(2).split(" "); return /*#__PURE__*/React.createElement("circle", {
  key: i,
  cx: cx,
  cy: cy,
  r: "1.2",
  fill: color,
  stroke: "none"
}); })()
          : /*#__PURE__*/React.createElement("path", {
  key: i,
  d: p,
  fill: solid && p.trim().endsWith("Z") ? color : "none"
})
      ));
}

const FOS_ICON_NAMES = Object.keys(P).sort();
FosIcon.names = FOS_ICON_NAMES;
Object.assign(__ds_scope, { FosIcon, FOS_ICON_NAMES });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/FosIcon.jsx", error: String((e && e.message) || e) }); }

// components/core/ActionGrid.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* ActionGrid (FutureActionCell): the focusable icon+label cells that carry a screen's
   actions — four in a row under a translation, 2x2 as call controls, a column of contact
   actions. Cell = 20% accent fill when active, 4px accent border when focused, a 44%-of-height
   glyph (20-40dp) over an optional 13sp label. `columns` sets the shape; `height` the cell.
   Two variants of the glyph: a FosIcon (`icon`), or a text symbol (`glyph`: "+", "sin", "π")
   drawn at 90% of the icon size in the same 70% alpha, accent when active. A `legend` cell is
   the second variant made non-focusable: a reminder of what a physical key does (glyph = the
   operation, label = the key name), so it never takes the focus border and never activates. */
function ActionGrid({
  items = [],
  focusedIndex = -1,
  columns = 2,
  height = 132,
  legend = false,
  onSelect,
  style,
  ...rest
}) {
  const iconPx = Math.max(40, Math.min(80, Math.round(height * 0.44)));
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      display: "grid",
      gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))`,
      gap: "var(--fos-space-3)",
      ...style
    }
  }, rest), items.map((it, i) => {
    const isLegend = it.legend ?? legend;
    const focused = focusedIndex === i && !isLegend;
    const ink = it.color || (it.active ? "var(--fos-accent)" : "var(--fos-text-70)");
    return /*#__PURE__*/React.createElement("div", {
      key: (it.label || it.glyph || "") + i,
      onClick: onSelect && !isLegend ? () => onSelect(i) : undefined,
      style: {
        height,
        borderRadius: "var(--fos-radius-card)",
        background: it.active ? "var(--fos-accent-20)" : "var(--fos-glass)",
        border: `4px solid ${focused ? "var(--fos-accent)" : "transparent"}`,
        boxSizing: "border-box",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        gap: "var(--fos-space-1)",
        cursor: isLegend ? "default" : "pointer",
        transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)"
      }
    }, it.glyph != null ? /*#__PURE__*/React.createElement("div", {
      style: {
        direction: "ltr",
        fontSize: Math.round(iconPx * 0.9),
        lineHeight: 1,
        fontWeight: "var(--fos-weight-regular)",
        color: ink,
        textAlign: "center",
        whiteSpace: "nowrap"
      }
    }, it.glyph) : /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
      name: it.icon,
      size: iconPx,
      strokeWidth: 1.3,
      color: ink
    }), it.label && /*#__PURE__*/React.createElement("div", {
      style: {
        maxWidth: "100%",
        fontSize: "var(--fos-size-summary)",
        color: "var(--fos-text-60)",
        textAlign: "center",
        whiteSpace: "nowrap",
        overflow: "hidden",
        textOverflow: "ellipsis"
      }
    }, it.label));
  }));
}
Object.assign(__ds_scope, { ActionGrid });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/ActionGrid.jsx", error: String((e && e.message) || e) }); }

// components/core/Avatar.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* Avatar: the circle every person- or device-facing surface repeats — glass fill,
   full radius, and either a glyph at 44% of the circle or the name's initials at
   30% in the display face. Sizes are free; 88 is the list size, 176 the hero size. */
function initials(name) {
  const parts = String(name || "").trim().split(/\s+/).filter(Boolean).slice(0, 2);
  return parts.map(p => p[0]).join("");
}
function Avatar({
  name,
  icon,
  size = 88,
  color,
  background,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      width: size,
      height: size,
      flex: "0 0 auto",
      borderRadius: "var(--fos-radius-full)",
      background: background || "var(--fos-avatar-fill)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      ...style
    }
  }, rest), icon ? /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
    name: icon,
    size: Math.round(size * 0.44),
    color: color || "var(--fos-text)"
  }) : /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: "var(--fos-font-display)",
      fontSize: Math.round(size * 0.3),
      fontWeight: "var(--fos-weight-medium)",
      color: color || "var(--fos-text)"
    }
  }, initials(name)));
}
Avatar.initials = initials;
Object.assign(__ds_scope, { Avatar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Avatar.jsx", error: String((e && e.message) || e) }); }

// components/core/EmptyState.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* Two lines and one 56dp outlined glyph, all centred, all in the text color at
   fixed alphas: glyph 40%, title 70% at 17sp/500, subtitle 40% at 14sp/400. */
function EmptyState({
  icon = "inbox",
  title,
  subtitle,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      gap: "var(--fos-space-3)",
      padding: "var(--fos-space-10)",
      textAlign: "center",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
    name: icon,
    size: 112,
    color: "var(--fos-text)",
    opacity: 0.4,
    strokeWidth: 1.1
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-title)",
      fontWeight: "var(--fos-weight-medium)",
      color: "var(--fos-text-70)",
      marginTop: "var(--fos-space-1)"
    }
  }, title), subtitle && /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-body)",
      color: "var(--fos-text-40)"
    }
  }, subtitle));
}
Object.assign(__ds_scope, { EmptyState });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/EmptyState.jsx", error: String((e && e.message) || e) }); }

// components/core/IconButton.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* TopBarIconButton: 36dp circle, 8% text background idle, 30% accent when focused,
   18dp glyph in the text color. */
function IconButton({
  icon,
  focused = false,
  color = "var(--fos-text)",
  onClick,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    onClick: onClick,
    style: {
      width: "var(--fos-row-topbar-btn)",
      height: "var(--fos-row-topbar-btn)",
      flex: "0 0 auto",
      borderRadius: "var(--fos-radius-full)",
      border: "none",
      background: focused ? "var(--fos-focus-bg-icon)" : "var(--fos-idle-bg-icon)",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      cursor: "pointer",
      transition: "background var(--fos-transition-focus)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
    name: icon,
    size: 36,
    color: color
  }));
}
Object.assign(__ds_scope, { IconButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/IconButton.jsx", error: String((e && e.message) || e) }); }

// components/core/ListItem.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* FocusableItem: 112px row, 4dp inner padding, title 17sp/500, summary 13sp/60%.
   Focus = 14% accent background + 1.5dp accent border + 1.02 scale. */
function ListItem({
  title,
  summary,
  trailing,
  focused = false,
  onClick,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    onClick: onClick,
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      display: "flex",
      alignItems: "center",
      gap: "var(--fos-space-5)",
      height: "var(--fos-row-list)",
      padding: "0 var(--fos-space-5)",
      borderRadius: "var(--fos-radius-row)",
      background: focused ? "var(--fos-focus-bg-item)" : "transparent",
      border: `var(--fos-focus-border-item) solid ${focused ? "var(--fos-accent)" : "transparent"}`,
      transform: focused ? "scale(var(--fos-focus-scale))" : "none",
      transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus), transform var(--fos-transition-focus)",
      cursor: "pointer",
      boxSizing: "border-box",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-title)",
      fontWeight: "var(--fos-weight-medium)",
      color: "var(--fos-text)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, title), summary && /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-summary)",
      color: "var(--fos-text-60)",
      marginTop: 4,
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, summary)), trailing);
}
Object.assign(__ds_scope, { ListItem });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/ListItem.jsx", error: String((e && e.message) || e) }); }

// components/core/MonoValue.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* MonoValue: a running number — call timer, stopwatch, world clock. Roboto Mono,
   tabular figures and LTR always, so the glyphs never shift as digits change.
   `size` is a token value; the three that recur are clock, header and title. */
function MonoValue({
  children,
  size = "var(--fos-size-header)",
  weight = 300,
  color = "var(--fos-text)",
  label,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: "var(--fos-space-1)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      fontFamily: "var(--fos-font-mono)",
      fontSize: size,
      fontWeight: weight,
      fontVariantNumeric: "tabular-nums",
      fontFeatureSettings: '"tnum"',
      direction: "ltr",
      color,
      lineHeight: 1.1
    }
  }, children), label && /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-summary)",
      color: "var(--fos-text-60)"
    }
  }, label));
}
Object.assign(__ds_scope, { MonoValue });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/MonoValue.jsx", error: String((e && e.message) || e) }); }

// components/core/ScreenHero.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* ScreenHero: the centred identity block a detail screen opens with — a large
   Avatar, the name at 22sp/700 in the display face, and one status line at 14sp/60%.
   `eyebrow` is the 12sp tracked label above (שיחה נכנסת); `statusColor` carries the
   live state (success for a running call). Children sit under the status line. */
function ScreenHero({
  name,
  icon,
  eyebrow,
  status,
  statusColor,
  size = 176,
  children,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: "var(--fos-space-3)",
      padding: "var(--fos-space-5) var(--fos-space-screen) var(--fos-space-8)",
      textAlign: "center",
      ...style
    }
  }, rest), eyebrow && /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-label)",
      letterSpacing: "var(--fos-tracking-section)",
      color: "var(--fos-text-60)"
    }
  }, eyebrow), /*#__PURE__*/React.createElement(__ds_scope.Avatar, {
    name: name,
    icon: icon,
    size: size
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-screen-title)",
      fontWeight: "var(--fos-weight-bold)",
      fontFamily: "var(--fos-font-display)",
      textWrap: "pretty"
    }
  }, name), status && /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-body)",
      color: statusColor || "var(--fos-text-60)"
    }
  }, status), children);
}
Object.assign(__ds_scope, { ScreenHero });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/ScreenHero.jsx", error: String((e && e.message) || e) }); }

// components/core/SectionHeader.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* SettingHeader: 13sp / 700 / 55% text, 1sp letter-spacing, 24dp start, 20dp top, 8dp bottom. */
function SectionHeader({
  children,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font-display)",
      fontSize: "var(--fos-size-summary)",
      fontWeight: "var(--fos-weight-bold)",
      letterSpacing: "var(--fos-tracking-section)",
      color: "var(--fos-text-55)",
      padding: "var(--fos-space-8) var(--fos-space-9) var(--fos-space-3) var(--fos-space-9)",
      ...style
    }
  }, rest), children);
}
Object.assign(__ds_scope, { SectionHeader });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/SectionHeader.jsx", error: String((e && e.message) || e) }); }

// components/core/TopBar.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* ScreenTopBar: 16dp horizontal / 12dp vertical padding, 20sp/700 title, 36dp buttons.
   RTL — back sits at the right (the start), overflow at the left. */
function TopBar({
  title,
  onBack,
  onMenu,
  backFocused = false,
  menuFocused = false,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      display: "flex",
      alignItems: "center",
      gap: "var(--fos-space-3)",
      padding: "var(--fos-space-5) var(--fos-space-7)",
      ...style
    }
  }, rest), onBack && /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    icon: "arrow_forward",
    focused: backFocused,
    onClick: onBack
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0,
      fontFamily: "var(--fos-font-display)",
      fontSize: "var(--fos-size-screen-title)",
      fontWeight: "var(--fos-weight-bold)",
      color: "var(--fos-text)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, title), onMenu && /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    icon: "more_vert",
    focused: menuFocused,
    onClick: onMenu
  }));
}
Object.assign(__ds_scope, { TopBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/TopBar.jsx", error: String((e && e.message) || e) }); }

// components/feedback/Badge.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* 18dp accent circle with black 10sp/700 digits. Unread message count only. */
function Badge({
  count,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      minWidth: 36,
      height: 36,
      flex: "0 0 auto",
      padding: "0 8px",
      borderRadius: "var(--fos-radius-full)",
      background: "var(--fos-accent)",
      color: "var(--fos-on-accent)",
      fontFamily: "var(--fos-font)",
      fontSize: "var(--fos-size-badge)",
      fontWeight: "var(--fos-weight-bold)",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      boxSizing: "border-box",
      ...style
    }
  }, rest), count);
}
Object.assign(__ds_scope, { Badge });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/Badge.jsx", error: String((e && e.message) || e) }); }

// components/feedback/ConfirmDialog.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* ConfirmDialog: 85% of screen width, max 86% height, 20dp radius, 20dp padding,
   message 15sp/700 centred. Buttons are RTL: cancel on the right, the action on the
   left. The screen behind is dimmed with a 60% black scrim and focus is trapped. */
function ConfirmDialog({
  message,
  confirmLabel = "מחק",
  cancelLabel = "ביטול",
  destructive = true,
  focus = "confirm",
  onConfirm,
  onCancel,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      position: "absolute",
      inset: 0,
      background: "var(--fos-scrim)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      width: "85%",
      maxHeight: "86%",
      background: "var(--fos-surface-card)",
      borderRadius: "var(--fos-radius-dialog)",
      padding: "var(--fos-space-8)",
      boxSizing: "border-box",
      transform: "scale(var(--fos-focus-scale))"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-dialog)",
      fontWeight: "var(--fos-weight-bold)",
      color: "var(--fos-text)",
      textAlign: "center",
      lineHeight: 1.2
    }
  }, message), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: "var(--fos-space-5)",
      justifyContent: "center",
      marginTop: "var(--fos-space-7)"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Button, {
    variant: "secondary",
    focused: focus === "cancel",
    onClick: onCancel
  }, cancelLabel), /*#__PURE__*/React.createElement(__ds_scope.Button, {
    variant: destructive ? "destructive" : "primary",
    focused: focus === "confirm",
    onClick: onConfirm
  }, confirmLabel))));
}
Object.assign(__ds_scope, { ConfirmDialog });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/ConfirmDialog.jsx", error: String((e && e.message) || e) }); }

// components/feedback/HeadsUpNotification.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* HeadsUpNotificationScreen: floats over everything, inset 12dp from the sides and
   8dp from the top. 28dp radius, #1C1C1E at 90%, 0.5dp white hairline at 15%.
   Always dark, even when the system is in light mode — hence the literal colors. */
function HeadsUpNotification({
  appName,
  title,
  body,
  icon = "chat",
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      margin: "var(--fos-space-3) var(--fos-space-5)",
      padding: "var(--fos-space-4) var(--fos-space-6)",
      borderRadius: "var(--fos-radius-headsup)",
      background: "var(--fos-headsup-bg)",
      border: "var(--fos-border-headsup) solid rgba(255,255,255,0.15)",
      boxShadow: "var(--fos-shadow-headsup)",
      display: "flex",
      alignItems: "center",
      gap: "var(--fos-space-5)",
      boxSizing: "border-box",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      width: 68,
      height: 68,
      flex: "0 0 auto",
      borderRadius: "var(--fos-radius-full)",
      background: "rgba(255,255,255,0.12)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
    name: icon,
    size: 36,
    color: "#FFFFFF"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-label)",
      color: "rgba(255,255,255,0.65)"
    }
  }, appName), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-dialog)",
      fontWeight: "var(--fos-weight-bold)",
      color: "#FFFFFF",
      marginTop: 2
    }
  }, title), body && /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-summary)",
      color: "rgba(255,255,255,0.55)",
      marginTop: 4,
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, body)));
}
Object.assign(__ds_scope, { HeadsUpNotification });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/HeadsUpNotification.jsx", error: String((e && e.message) || e) }); }

// components/feedback/ProgressBar.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* LinearProgressIndicator: 4dp tall (2dp in the music player), 10% text track,
   accent fill growing from the RIGHT. */
function ProgressBar({
  value = 0,
  mini = false,
  style,
  ...rest
}) {
  const h = mini ? 4 : 8;
  const pct = Math.max(0, Math.min(1, value)) * 100;
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      height: h,
      borderRadius: h / 2,
      background: "var(--fos-text-10)",
      overflow: "hidden",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      width: pct + "%",
      height: "100%",
      borderRadius: h / 2,
      background: "var(--fos-accent)",
      transition: "width var(--fos-duration-standard) var(--fos-ease-linear)"
    }
  }));
}
Object.assign(__ds_scope, { ProgressBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/ProgressBar.jsx", error: String((e && e.message) || e) }); }

// components/feedback/Snackbar.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* Snackbar: transient feedback for something that already happened, anchored to
   the bottom of the screen — where HeadsUpNotification is an incoming event at the
   top. Card surface, dialog radius, 15sp message, one optional accent action label
   (the action is reached with the left soft key, so it is a label, not a button). */
function Snackbar({
  message,
  action,
  onAction,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      margin: "0 var(--fos-space-5) var(--fos-space-5)",
      padding: "var(--fos-space-5) var(--fos-space-6)",
      borderRadius: "var(--fos-radius-dialog)",
      background: "var(--fos-surface-card)",
      boxShadow: "var(--fos-shadow-headsup)",
      display: "flex",
      alignItems: "center",
      gap: "var(--fos-space-6)",
      boxSizing: "border-box",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0,
      fontSize: "var(--fos-size-dialog)",
      color: "var(--fos-text)",
      textWrap: "pretty"
    }
  }, message), action && /*#__PURE__*/React.createElement("div", {
    onClick: onAction,
    style: {
      flex: "0 0 auto",
      fontSize: "var(--fos-size-dialog)",
      fontWeight: "var(--fos-weight-bold)",
      color: "var(--fos-accent)",
      cursor: "pointer"
    }
  }, action));
}
Object.assign(__ds_scope, { Snackbar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/Snackbar.jsx", error: String((e && e.message) || e) }); }

// components/feedback/Spinner.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* Spinner: the circular counterpart to ProgressBar, for waits with no known
   length (scanning, connecting, sending). 10% text track, a round-capped accent
   arc over a quarter of the circle, one rotation per 900ms — the only rotating
   element in the system. */
const KEYFRAMES = "@keyframes fos-spin{to{transform:rotate(360deg)}}";
function Spinner({
  size = 72,
  thickness = 8,
  label,
  style,
  ...rest
}) {
  const r = (size - thickness) / 2;
  const c = 2 * Math.PI * r;
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      display: "flex",
      alignItems: "center",
      gap: "var(--fos-space-5)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("style", null, KEYFRAMES), /*#__PURE__*/React.createElement("svg", {
    width: size,
    height: size,
    viewBox: `0 0 ${size} ${size}`,
    style: {
      flex: "0 0 auto",
      animation: "fos-spin 900ms var(--fos-ease-linear) infinite"
    }
  }, /*#__PURE__*/React.createElement("circle", {
    cx: size / 2,
    cy: size / 2,
    r: r,
    fill: "none",
    stroke: "var(--fos-text-10)",
    strokeWidth: thickness
  }), /*#__PURE__*/React.createElement("circle", {
    cx: size / 2,
    cy: size / 2,
    r: r,
    fill: "none",
    stroke: "var(--fos-accent)",
    strokeWidth: thickness,
    strokeLinecap: "round",
    strokeDasharray: `${c * 0.26} ${c}`,
    transform: `rotate(-90 ${size / 2} ${size / 2})`
  })), label && /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-body)",
      color: "var(--fos-text-60)"
    }
  }, label));
}
Object.assign(__ds_scope, { Spinner });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/Spinner.jsx", error: String((e && e.message) || e) }); }

// components/forms/Capsule.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* Capsule: a pill-shaped focusable control that carries a small label above its
   value — the language pickers in תרגום, and, at `round`, the big mic button.
   Focus is the 4px accent border the app surfaces use, not Button's white one. */
function Capsule({
  label,
  children,
  icon,
  focused = false,
  active = false,
  round = false,
  size = 168,
  onClick,
  style,
  ...rest
}) {
  const fill = active ? "var(--fos-accent-20)" : "var(--fos-text-08)";
  const glyph = active ? "var(--fos-accent)" : "var(--fos-text-60)";
  if (round) {
    return /*#__PURE__*/React.createElement("div", _extends({
      onClick: onClick,
      style: {
        width: size,
        height: size,
        flex: "0 0 auto",
        borderRadius: "var(--fos-radius-full)",
        background: fill,
        border: `4px solid ${focused ? "var(--fos-accent)" : "transparent"}`,
        boxSizing: "border-box",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        cursor: "pointer",
        transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
        ...style
      }
    }, rest), /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
      name: icon,
      size: Math.round(size * 0.48),
      color: glyph
    }));
  }
  return /*#__PURE__*/React.createElement("div", _extends({
    onClick: onClick,
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: 2,
      padding: "var(--fos-space-3) var(--fos-space-6)",
      borderRadius: "var(--fos-radius-full)",
      background: fill,
      border: `4px solid ${focused ? "var(--fos-accent)" : "transparent"}`,
      boxSizing: "border-box",
      cursor: "pointer",
      transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
      ...style
    }
  }, rest), label && /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-label)",
      color: "var(--fos-text-55)",
      letterSpacing: "var(--fos-tracking-section)"
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: "var(--fos-space-2)",
      fontSize: "var(--fos-size-title)",
      fontWeight: "var(--fos-weight-medium)",
      color: active ? "var(--fos-accent)" : "var(--fos-text)"
    }
  }, icon && /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
    name: icon,
    size: 36,
    color: active ? "var(--fos-accent)" : "var(--fos-text-60)"
  }), children));
}
Object.assign(__ds_scope, { Capsule });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Capsule.jsx", error: String((e && e.message) || e) }); }

// components/forms/Checkbox.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* Checkbox: multi-select, for the trailing slot of a SettingItem or ListItem
   (bulk-delete a thread, pick which days to sync). Checked = solid accent with a
   black check, the same fill/ink pair the selected chip uses. Unchecked is an
   empty box with a 40% text border, so it survives a white accent. */
function Checkbox({
  checked = false,
  size = 48,
  style,
  onChange,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    onClick: onChange,
    role: "checkbox",
    "aria-checked": checked,
    style: {
      width: size,
      height: size,
      flex: "0 0 auto",
      borderRadius: "var(--fos-radius-item)",
      background: checked ? "var(--fos-accent)" : "transparent",
      border: checked ? "var(--fos-focus-border-control) solid var(--fos-accent)" : "var(--fos-focus-border-control) solid var(--fos-text-40)",
      boxSizing: "border-box",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      cursor: "pointer",
      transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
      ...style
    }
  }, rest), checked && /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
    name: "check",
    size: Math.round(size * 0.72),
    color: "var(--fos-on-accent)",
    strokeWidth: 2.2
  }));
}
Object.assign(__ds_scope, { Checkbox });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Checkbox.jsx", error: String((e && e.message) || e) }); }

// components/forms/Chip.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* GalleryTabChip: 14dp radius, 18/8dp padding, 13sp/500.
   selected = solid accent with black text; focused = 18% text; idle = 6% text. */
function Chip({
  children,
  state = "idle",
  onClick,
  style,
  ...rest
}) {
  const selected = state === "selected";
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    onClick: onClick,
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      fontSize: "var(--fos-size-summary)",
      fontWeight: "var(--fos-weight-medium)",
      lineHeight: 1.25,
      color: selected ? "var(--fos-on-accent)" : "var(--fos-text)",
      background: selected ? "var(--fos-accent)" : state === "focused" ? "var(--fos-focus-bg-chip)" : "var(--fos-idle-bg-chip)",
      border: "none",
      borderRadius: "var(--fos-radius-chip)",
      padding: "var(--fos-space-3) var(--fos-space-4)",
      paddingInline: 36,
      cursor: "pointer",
      flex: "0 0 auto",
      transition: "background var(--fos-transition-focus), color var(--fos-transition-focus)",
      ...style
    }
  }, rest), children);
}
Object.assign(__ds_scope, { Chip });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Chip.jsx", error: String((e && e.message) || e) }); }

// components/forms/DatePicker.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* DatePicker: TimePickerOverlay's sibling — a full-screen overlay, not a dialog.
   Month stepper at the top, then a 7-column grid that flows right-to-left from
   Sunday. Selected = solid accent with black text (the system's selected state);
   focused = 2dp accent ring. Weeks are computed from year/month; nothing is
   stateful, the screen owns the cursor. */
const DAYS = ["א", "ב", "ג", "ד", "ה", "ו", "ש"];
const MONTHS = ["ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני", "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר"];
function DatePicker({
  title = "בחר תאריך",
  year = 2026,
  month = 1,
  selected,
  focused,
  onPrevMonth,
  onNextMonth,
  onCancel,
  onSave,
  onSelect,
  style,
  ...rest
}) {
  const first = new Date(year, month - 1, 1).getDay();
  const count = new Date(year, month, 0).getDate();
  const cells = [];
  for (let i = 0; i < first; i++) cells.push(null);
  for (let d = 1; d <= count; d++) cells.push(d);
  const step = {
    width: 72,
    height: 72,
    borderRadius: "var(--fos-radius-full)",
    background: "var(--fos-text-08)",
    border: "none",
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    cursor: "pointer"
  };
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      background: "var(--fos-bg)",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      gap: "var(--fos-space-8)",
      padding: "var(--fos-space-7)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-screen-title)",
      fontWeight: "var(--fos-weight-bold)"
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: "var(--fos-space-7)"
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    style: step,
    onClick: onNextMonth
  }, /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
    name: "keyboard_arrow_left",
    size: 36,
    color: "var(--fos-accent)"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-title)",
      fontWeight: "var(--fos-weight-medium)",
      minWidth: 260,
      textAlign: "center"
    }
  }, MONTHS[month - 1], " ", year), /*#__PURE__*/React.createElement("button", {
    type: "button",
    style: step,
    onClick: onPrevMonth
  }, /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
    name: "keyboard_arrow_right",
    size: 36,
    color: "var(--fos-accent)"
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      width: "100%",
      display: "flex",
      flexDirection: "column",
      gap: "var(--fos-space-2)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(7, 1fr)",
      gap: "var(--fos-space-2)"
    }
  }, DAYS.map(d => /*#__PURE__*/React.createElement("div", {
    key: d,
    style: {
      textAlign: "center",
      fontSize: "var(--fos-size-label)",
      color: "var(--fos-text-50)",
      letterSpacing: "var(--fos-tracking-section)"
    }
  }, d))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(7, 1fr)",
      gap: "var(--fos-space-2)"
    }
  }, cells.map((d, i) => {
    if (d === null) return /*#__PURE__*/React.createElement("div", {
      key: "e" + i
    });
    const sel = d === selected;
    const foc = d === focused;
    return /*#__PURE__*/React.createElement("div", {
      key: d,
      onClick: onSelect ? () => onSelect(d) : undefined,
      style: {
        aspectRatio: "1",
        borderRadius: "var(--fos-radius-full)",
        background: sel ? "var(--fos-accent)" : "transparent",
        border: `var(--fos-focus-border-control) solid ${foc && !sel ? "var(--fos-accent)" : "transparent"}`,
        boxSizing: "border-box",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontSize: "var(--fos-size-body)",
        fontWeight: sel ? "var(--fos-weight-bold)" : "var(--fos-weight-regular)",
        color: sel ? "var(--fos-on-accent)" : "var(--fos-text)",
        fontVariantNumeric: "tabular-nums",
        cursor: "pointer",
        transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)"
      }
    }, d);
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: "var(--fos-space-7)",
      width: "100%"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Button, {
    variant: "quiet",
    fullWidth: true,
    onClick: onCancel
  }, "\u05D1\u05D9\u05D8\u05D5\u05DC"), /*#__PURE__*/React.createElement(__ds_scope.Button, {
    variant: "primary",
    fullWidth: true,
    focused: true,
    onClick: onSave
  }, "\u05E9\u05DE\u05D5\u05E8")));
}
Object.assign(__ds_scope, { DatePicker });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/DatePicker.jsx", error: String((e && e.message) || e) }); }

// components/forms/DayChip.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* DayToggleChip: 36dp circle, 14sp/700. selected = accent + black;
   focused = 20% text with a 2dp accent ring; idle = 8% text. */
function DayChip({
  children,
  selected = false,
  focused = false,
  onClick,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    onClick: onClick,
    style: {
      width: 72,
      height: 72,
      flex: "0 0 auto",
      borderRadius: "var(--fos-radius-full)",
      fontFamily: "var(--fos-font)",
      fontSize: "var(--fos-size-body)",
      fontWeight: "var(--fos-weight-bold)",
      color: selected ? "var(--fos-on-accent)" : "var(--fos-text)",
      background: selected ? "var(--fos-accent)" : focused ? "var(--fos-accent-20)" : "var(--fos-text-08)",
      border: `var(--fos-focus-border-control) solid ${focused && !selected ? "var(--fos-accent)" : "transparent"}`,
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      cursor: "pointer",
      boxSizing: "border-box",
      transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
      ...style
    }
  }, rest), children);
}
Object.assign(__ds_scope, { DayChip });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/DayChip.jsx", error: String((e && e.message) || e) }); }

// components/forms/RadioButton.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* RadioButton: one-of-many, for the trailing slot of a row (accent picker,
   ringtone, text size). Ring at 40% text when off, accent ring with a filled
   accent dot when on — never a fill, so a white accent still reads as a ring. */
function RadioButton({
  selected = false,
  size = 48,
  style,
  onChange,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    onClick: onChange,
    role: "radio",
    "aria-checked": selected,
    style: {
      width: size,
      height: size,
      flex: "0 0 auto",
      borderRadius: "var(--fos-radius-full)",
      border: `var(--fos-focus-border-control) solid ${selected ? "var(--fos-accent)" : "var(--fos-text-40)"}`,
      boxSizing: "border-box",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      cursor: "pointer",
      transition: "border-color var(--fos-transition-focus)",
      ...style
    }
  }, rest), selected && /*#__PURE__*/React.createElement("div", {
    style: {
      width: Math.round(size * 0.5),
      height: Math.round(size * 0.5),
      borderRadius: "var(--fos-radius-full)",
      background: "var(--fos-accent)"
    }
  }));
}
Object.assign(__ds_scope, { RadioButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/RadioButton.jsx", error: String((e && e.message) || e) }); }

// components/forms/SettingItem.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* SettingItem: 108px row inside a Card. 4dp outer / 12dp inner horizontal padding,
   22dp accent icon, 16dp gap, 18dp chevron at 30%, title 17sp/600, summary 13sp/60%.
   Focus = 6% text background + 2dp accent border. No scale.
   The focus fills the full width of the card: the first row keeps the card's top
   corners, the last row its bottom corners, a middle row none. Card assigns this
   automatically to the SettingItem rows inside it; pass `position` to override. */
const CARD_R = "var(--fos-radius-main)";
const focusRadius = {
  only: CARD_R,
  first: `${CARD_R} ${CARD_R} 0 0`,
  last: `0 0 ${CARD_R} ${CARD_R}`,
  middle: "0"
};
const bleed = "calc(-1 * var(--fos-space-1))"; /* eats the card's 6dp vertical padding */

function SettingItem({
  title,
  summary,
  icon,
  trailing,
  chevron = true,
  focused = false,
  position = "only",
  onClick,
  style,
  ...rest
}) {
  const top = position === "first" || position === "only" ? bleed : "0";
  const bottom = position === "last" || position === "only" ? bleed : "0";
  return /*#__PURE__*/React.createElement("div", _extends({
    onClick: onClick,
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      display: "flex",
      alignItems: "center",
      gap: "var(--fos-space-7)",
      height: "var(--fos-row-setting)",
      margin: `${top} 0 ${bottom}`,
      padding: "0 var(--fos-space-7)",
      borderRadius: focusRadius[position] || CARD_R,
      background: focused ? "var(--fos-focus-bg-setting)" : "transparent",
      border: `var(--fos-focus-border-control) solid ${focused ? "var(--fos-accent)" : "transparent"}`,
      boxSizing: "border-box",
      cursor: "pointer",
      transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
      ...style
    }
  }, rest), icon && /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
    name: icon,
    size: 44,
    color: "var(--fos-accent)"
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-title)",
      fontWeight: "var(--fos-weight-semibold)",
      color: "var(--fos-text)"
    }
  }, title), summary && /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-summary)",
      color: "var(--fos-text-60)",
      marginTop: 4
    }
  }, summary)), trailing, !trailing && chevron && /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
    name: "chevron_left",
    size: 36,
    color: "var(--fos-text)",
    opacity: 0.3
  }));
}
SettingItem.__fosCardRow = true;
Object.assign(__ds_scope, { SettingItem });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/SettingItem.jsx", error: String((e && e.message) || e) }); }

// components/forms/Slider.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* VolumeSlider / FutureSlider: full-width row at 16dp card radius, 16dp inner padding,
   optional leading icon (20dp, 60% text) before the label 14sp/60%, 6dp track at 15% text,
   accent fill starting from the RIGHT. Left/right keys step 5%.
   Focus = 18% text background + 2dp accent border. Used in Settings and in the Control Center. */
function Slider({
  label,
  icon,
  value = 0.5,
  focused = false,
  style,
  ...rest
}) {
  const pct = Math.max(0, Math.min(1, value)) * 100;
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      margin: "0 var(--fos-space-7)",
      padding: "var(--fos-space-7)",
      borderRadius: "var(--fos-radius-card)",
      background: focused ? "var(--fos-focus-bg-slider)" : "var(--fos-idle-bg-slider)",
      border: `var(--fos-focus-border-control) solid ${focused ? "var(--fos-accent)" : "transparent"}`,
      boxSizing: "border-box",
      transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: "var(--fos-space-3)",
      fontSize: "var(--fos-size-body)",
      color: "var(--fos-text-60)",
      marginBottom: "var(--fos-space-5)"
    }
  }, icon && /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
    name: icon,
    size: 40,
    color: "var(--fos-text-60)"
  }), /*#__PURE__*/React.createElement("span", null, label)), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 12,
      borderRadius: 6,
      background: "var(--fos-text-15)",
      overflow: "hidden"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: pct + "%",
      height: "100%",
      borderRadius: 6,
      background: "var(--fos-accent)",
      transition: "width var(--fos-transition-focus)"
    }
  })));
}
Object.assign(__ds_scope, { Slider });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Slider.jsx", error: String((e && e.message) || e) }); }

// components/forms/Switch.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* SettingSwitch. The Kotlin source sets only the colors, and its thumb is white in
   both states — which, with the default white accent, makes an "on" switch read as a
   plain white pill. This version keeps Material's geometry but takes the thumb out of
   the accent's way: off is an empty track with a 30% hairline and a 40% thumb, on is
   the accent-filled track with the thumb in the screen color. Nothing here depends on
   the accent being chromatic.
   Position is logical, so in RTL the thumb rests at the right and travels left. */
const TRACK_W = 104;
const TRACK_H = 60;
const PAD = 8;
const THUMB_OFF = 28;
const THUMB_ON = 36;
function Switch({
  on = false,
  onChange,
  style,
  ...rest
}) {
  const size = on ? THUMB_ON : THUMB_OFF;
  return /*#__PURE__*/React.createElement("div", _extends({
    onClick: onChange,
    role: "switch",
    "aria-checked": on,
    style: {
      width: TRACK_W,
      height: TRACK_H,
      flex: "0 0 auto",
      borderRadius: "var(--fos-radius-full)",
      background: on ? "var(--fos-accent)" : "transparent",
      border: `var(--fos-focus-border-control) solid ${on ? "var(--fos-accent)" : "var(--fos-text-30)"}`,
      boxSizing: "border-box",
      position: "relative",
      cursor: "pointer",
      transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: "50%",
      marginTop: -size / 2,
      insetInlineStart: on ? TRACK_W - size - PAD - 4 : PAD,
      width: size,
      height: size,
      borderRadius: "var(--fos-radius-full)",
      background: on ? "var(--fos-bg)" : "var(--fos-text-40)",
      transition: "all var(--fos-transition-focus)"
    }
  }));
}
Object.assign(__ds_scope, { Switch });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Switch.jsx", error: String((e && e.message) || e) }); }

// components/forms/TextArea.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* TextArea: TextField's surface grown to a paragraph — same 8% fill, card radius
   and accent focus border, but 16sp text on the body line-height, a minimum height
   and top alignment. `dir`/`align` let a field hold a foreign-language string. */
function TextArea({
  value,
  placeholder,
  focused = false,
  showCaret = true,
  minHeight = 150,
  dir = "rtl",
  align,
  style,
  onClick,
  ...rest
}) {
  const empty = !value;
  return /*#__PURE__*/React.createElement("div", _extends({
    onClick: onClick,
    style: {
      fontFamily: "var(--fos-font)",
      fontSize: "var(--fos-size-base)",
      lineHeight: "var(--fos-line-height)",
      color: empty ? "var(--fos-text-40)" : "var(--fos-text)",
      background: "var(--fos-idle-bg-field)",
      borderRadius: "var(--fos-radius-textfield)",
      border: `var(--fos-focus-border-control) solid ${focused ? "var(--fos-accent)" : "transparent"}`,
      padding: "var(--fos-space-5)",
      minHeight,
      boxSizing: "border-box",
      direction: dir,
      textAlign: align || (dir === "rtl" ? "right" : "left"),
      textWrap: "pretty",
      cursor: onClick ? "pointer" : "default",
      transition: "border-color var(--fos-transition-focus)",
      ...style
    }
  }, rest), empty ? placeholder : value, focused && showCaret && /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-block",
      width: 3,
      height: 34,
      background: "var(--fos-accent)",
      verticalAlign: "-6px",
      marginInlineStart: 6
    }
  }));
}
Object.assign(__ds_scope, { TextArea });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/TextArea.jsx", error: String((e && e.message) || e) }); }

// components/forms/TextField.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* BasicTextField: 8% text background, card radius, 12dp padding, 15sp text,
   2dp accent focus border, and a 3px accent caret. */
function TextField({
  value,
  placeholder,
  focused = false,
  showCaret = true,
  style,
  ...rest
}) {
  const empty = !value;
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      fontSize: "var(--fos-size-dialog)",
      color: empty ? "var(--fos-text-40)" : "var(--fos-text)",
      background: "var(--fos-idle-bg-field)",
      borderRadius: "var(--fos-radius-textfield)",
      border: `var(--fos-focus-border-control) solid ${focused ? "var(--fos-accent)" : "transparent"}`,
      padding: "var(--fos-space-5)",
      display: "flex",
      alignItems: "center",
      gap: 8,
      boxSizing: "border-box",
      transition: "border-color var(--fos-transition-focus)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("span", {
    style: {
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, empty ? placeholder : value), focused && showCaret && /*#__PURE__*/React.createElement("span", {
    style: {
      width: 3,
      height: 36,
      background: "var(--fos-accent)",
      flex: "0 0 auto"
    }
  }));
}
Object.assign(__ds_scope, { TextField });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/TextField.jsx", error: String((e && e.message) || e) }); }

// components/feedback/InputDialog.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* Same shell as ConfirmDialog with a field in the middle. The source's files-app
   version omits the focus border on its buttons; this follows ConfirmDialog, which
   is the shared and correct one. */
function InputDialog({
  title,
  value,
  placeholder,
  confirmLabel = "שמור",
  cancelLabel = "ביטול",
  onConfirm,
  onCancel,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      position: "absolute",
      inset: 0,
      background: "var(--fos-scrim)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      width: "85%",
      background: "var(--fos-surface-card)",
      borderRadius: "var(--fos-radius-dialog)",
      padding: "var(--fos-space-8)",
      boxSizing: "border-box",
      transform: "scale(var(--fos-focus-scale))"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-dialog)",
      fontWeight: "var(--fos-weight-bold)",
      color: "var(--fos-text)",
      textAlign: "center",
      marginBottom: "var(--fos-space-7)"
    }
  }, title), /*#__PURE__*/React.createElement(__ds_scope.TextField, {
    value: value,
    placeholder: placeholder,
    focused: true
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: "var(--fos-space-5)",
      justifyContent: "center",
      marginTop: "var(--fos-space-7)"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Button, {
    variant: "secondary",
    onClick: onCancel
  }, cancelLabel), /*#__PURE__*/React.createElement(__ds_scope.Button, {
    variant: "primary",
    onClick: onConfirm
  }, confirmLabel))));
}
Object.assign(__ds_scope, { InputDialog });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/InputDialog.jsx", error: String((e && e.message) || e) }); }

// components/forms/TimePicker.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const DAYS = ["א", "ב", "ג", "ד", "ה", "ו", "ש"];

/* TimePickerOverlay: a full-screen overlay, not a dialog. Two wheels — hours on
   the RIGHT (RTL first column), minutes on the left. A wheel is ONE focus target:
   field fill (8% text) at rest, 14% accent + 2px-dp accent ring when focused.
   Inside: the next value (above) and previous value (below) at 30%, the value
   itself at 48sp/300 mono, and a 12sp/50% unit label. Up/down step the focused
   wheel, digits type the value directly, OK moves on to the repeat row. */
function Wheel({ value, max, label, focused }) {
  const pad = (n) => String(n).padStart(2, "0");
  const v = Number(value);
  const next = v === max ? 0 : v + 1;
  const prev = v === 0 ? max : v - 1;
  const side = {
    fontFamily: "var(--fos-font-mono)", fontSize: "calc(48px * var(--fos-font-scale))" /* 24sp headline */,
    color: "var(--fos-text-30)", lineHeight: 1.2, fontVariantNumeric: "tabular-nums"
  };
  return /*#__PURE__*/React.createElement("div", {
  style: {
        width: 208, display: "flex", flexDirection: "column", alignItems: "center",
        padding: "var(--fos-space-3) 0", borderRadius: "var(--fos-radius-textfield)",
        background: focused ? "var(--fos-accent-14)" : "var(--fos-text-08)",
        boxShadow: focused ? "inset 0 0 0 4px var(--fos-accent)" : "none"
      }
}, /*#__PURE__*/React.createElement("div", {
  style: side
}, pad(next)), /*#__PURE__*/React.createElement("div", {
  style: VALUE
}, pad(v)), /*#__PURE__*/React.createElement("div", {
  style: side
}, pad(prev)), /*#__PURE__*/React.createElement("div", {
  style: UNIT_LABEL
}, label));
}

const VALUE = {
  fontFamily: "var(--fos-font-mono)", fontSize: "var(--fos-size-clock)",
  fontWeight: "var(--fos-weight-light)", color: "var(--fos-text)", lineHeight: 1,
  fontVariantNumeric: "tabular-nums", textAlign: "center"
};
const UNIT_LABEL = { fontSize: "var(--fos-size-label)", color: "var(--fos-text-50)", textAlign: "center" };

function TimePicker({ title = "ערוך שעה", hours = "07", minutes = "18", repeat = [0, 1, 2, 3, 4], focusedWheel = "hours", focusedDay = -1, onCancel, onSave, style, ...rest }) {
  return /*#__PURE__*/React.createElement("div", _extends({
  style: {
          direction: "rtl",
          fontFamily: "var(--fos-font)",
          background: "var(--fos-bg)",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          gap: "var(--fos-space-7)",
          padding: "var(--fos-space-7)",
          ...style
        }
}, rest), /*#__PURE__*/React.createElement("div", {
  style: { fontSize: "var(--fos-size-screen-title)", fontWeight: "var(--fos-weight-bold)", color: "var(--fos-text)" }
}, title), /*#__PURE__*/React.createElement("div", {
  style: { display: "flex", alignItems: "center", gap: "var(--fos-space-3)" }
}, /*#__PURE__*/React.createElement(Wheel, {
  value: hours,
  max: 23,
  label: "שעות",
  focused: focusedWheel === "hours"
}), /*#__PURE__*/React.createElement("div", {
  style: VALUE
}, ":"), /*#__PURE__*/React.createElement(Wheel, {
  value: minutes,
  max: 59,
  label: "דקות",
  focused: focusedWheel === "minutes"
})), /*#__PURE__*/React.createElement("div", {
  style: { fontSize: "var(--fos-size-summary)", color: "var(--fos-text-60)" }
}, "↑↓ שינוי · ספרות הקלדה · OK הבא"), /*#__PURE__*/React.createElement("div", {
  style: { display: "flex", flexDirection: "column", alignItems: "center", gap: "var(--fos-space-3)" }
}, /*#__PURE__*/React.createElement("div", {
  style: { fontSize: "var(--fos-size-summary)", color: "var(--fos-text-60)" }
}, repeat.length ? "חוזרת" : "חד-פעמית"), /*#__PURE__*/React.createElement("div", {
  style: { display: "flex", gap: "var(--fos-space-2)" }
}, DAYS.map((d, i) => /*#__PURE__*/React.createElement(__ds_scope.DayChip, {
  key: d,
  selected: repeat.includes(i),
  focused: i === focusedDay
}, d)))), /*#__PURE__*/React.createElement("div", {
  style: { display: "flex", gap: "var(--fos-space-7)", width: "100%" }
}, /*#__PURE__*/React.createElement(__ds_scope.Button, {
  variant: "quiet",
  fullWidth: true,
  onClick: onCancel
}, "ביטול"), /*#__PURE__*/React.createElement(__ds_scope.Button, {
  variant: "primary",
  fullWidth: true,
  onClick: onSave
}, "שמור")));
}
Object.assign(__ds_scope, { TimePicker });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/TimePicker.jsx", error: String((e && e.message) || e) }); }

// components/forms/ToggleButton.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* ToggleButton: a pressable that shows its own state — the third switch pattern,
   for when a Switch is too small and a Chip carries no state. Off is the glass
   surface at 70% text; on is the 20% accent fill with an accent glyph and label. */
function ToggleButton({
  children,
  icon,
  on = false,
  focused = false,
  fullWidth = false,
  onChange,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    onClick: onChange,
    role: "switch",
    "aria-checked": on,
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      fontSize: "var(--fos-size-base)",
      fontWeight: "var(--fos-weight-medium)",
      color: on ? "var(--fos-accent)" : "var(--fos-text-70)",
      background: on ? "var(--fos-accent-20)" : "var(--fos-glass)",
      border: `4px solid ${focused ? "var(--fos-accent)" : "transparent"}`,
      borderRadius: "var(--fos-radius-full)",
      padding: "0 var(--fos-space-7)",
      minHeight: 88,
      width: fullWidth ? "100%" : "auto",
      boxSizing: "border-box",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      gap: "var(--fos-space-3)",
      cursor: "pointer",
      transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus), color var(--fos-transition-focus)",
      ...style
    }
  }, rest), icon && /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
    name: icon,
    size: 40,
    color: on ? "var(--fos-accent)" : "var(--fos-text-60)"
  }), children);
}
Object.assign(__ds_scope, { ToggleButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/ToggleButton.jsx", error: String((e && e.message) || e) }); }

// components/navigation/BottomNav.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* FitnessBottomNav: a floating rounded bar inset from the screen edges. The selected
   item carries an accent pill behind a filled icon and is the only item that shows a
   label; unselected items are outline icons at 50% alpha.
   Deliberately NOT focusable — the screen-level arrow keys switch tabs. */
function BottomNav({
  items = [],
  selected = 0,
  onSelect,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      background: "var(--fos-surface-card)",
      borderRadius: "var(--fos-radius-full)",
      margin: "0 var(--fos-space-7) var(--fos-space-7)",
      height: 132,
      display: "flex",
      alignItems: "center",
      padding: "0 var(--fos-space-3)",
      boxSizing: "border-box",
      ...style
    }
  }, rest), items.map((it, i) => {
    const sel = i === selected;
    return /*#__PURE__*/React.createElement("div", {
      key: it.label,
      onClick: () => onSelect && onSelect(i),
      style: {
        flex: sel ? "1.4" : "1",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        cursor: "pointer",
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        height: 92,
        borderRadius: "var(--fos-radius-full)",
        background: sel ? "var(--fos-accent)" : "transparent",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        gap: "var(--fos-space-3)",
        padding: sel ? "0 var(--fos-space-6)" : "0",
        minWidth: 0,
        transition: "background var(--fos-transition-focus)"
      }
    }, /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
      name: it.icon,
      size: 40,
      color: sel ? "var(--fos-on-accent)" : "var(--fos-text)",
      opacity: sel ? 1 : 0.5,
      fill: sel ? 1 : 0
    }), sel && /*#__PURE__*/React.createElement("span", {
      style: {
        fontSize: "var(--fos-size-body)",
        fontWeight: "var(--fos-weight-medium)",
        color: "var(--fos-on-accent)",
        whiteSpace: "nowrap",
        overflow: "hidden",
        textOverflow: "ellipsis"
      }
    }, it.label)));
  }));
}
Object.assign(__ds_scope, { BottomNav });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/BottomNav.jsx", error: String((e && e.message) || e) }); }

// components/navigation/OptionsMenu.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* MenuRow inside the options overlay: opened by the hardware menu key.
   85% width, 20dp radius, 20/14dp row padding, 20dp icon, 14dp gap, 15sp label.
   Focus is a 12% text background only — no border, no scale. Destructive rows go red. */
function OptionsMenu({
  header,
  items = [],
  focusedIndex = -1,
  onSelect,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      position: "absolute",
      inset: 0,
      background: "var(--fos-scrim)",
      display: "flex",
      justifyContent: "center",
      alignItems: "flex-start",
      paddingTop: 80,
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      width: "85%",
      background: "var(--fos-surface-card)",
      borderRadius: "var(--fos-radius-dialog)",
      padding: "var(--fos-space-3) 0",
      overflow: "hidden"
    }
  }, header && /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-label)",
      color: "var(--fos-text-50)",
      padding: "var(--fos-space-2) var(--fos-space-8) var(--fos-space-3)"
    }
  }, header), items.map((it, i) => /*#__PURE__*/React.createElement("div", {
    key: it.label,
    onClick: () => onSelect && onSelect(i),
    style: {
      display: "flex",
      alignItems: "center",
      gap: "var(--fos-space-6)",
      height: "var(--fos-row-menu)",
      padding: "0 var(--fos-space-8)",
      background: i === focusedIndex ? "var(--fos-focus-bg-menu)" : "transparent",
      color: it.destructive ? "var(--fos-danger)" : "var(--fos-text)",
      cursor: "pointer",
      transition: "background var(--fos-transition-focus)"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
    name: it.icon,
    size: 40,
    color: "currentColor"
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-dialog)"
    }
  }, it.label)))));
}
Object.assign(__ds_scope, { OptionsMenu });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/OptionsMenu.jsx", error: String((e && e.message) || e) }); }

// components/navigation/SoftKeyBar.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* SoftKeyBar: the three labels along the bottom edge that name what the phone's
   two soft keys and the OK key do on this screen — the keypad equivalent of a
   bottom app bar, and the only always-visible affordance legend. Left and right
   sit against the edges, the centre label is the OK action in the accent color.
   Labels are single words; an unused key is left empty, never labelled "—". */
function SoftKeyBar({
  left,
  center,
  right,
  style,
  ...rest
}) {
  const side = {
    flex: 1,
    fontSize: "var(--fos-size-summary)",
    color: "var(--fos-text-60)",
    whiteSpace: "nowrap",
    overflow: "hidden",
    textOverflow: "ellipsis"
  };
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      display: "flex",
      alignItems: "center",
      gap: "var(--fos-space-5)",
      height: 72,
      padding: "0 var(--fos-space-7)",
      borderTop: "var(--fos-border-hairline) solid var(--fos-text-10)",
      background: "var(--fos-bg)",
      boxSizing: "border-box",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      ...side,
      textAlign: "right"
    }
  }, left), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: "0 0 auto",
      fontSize: "var(--fos-size-summary)",
      fontWeight: "var(--fos-weight-bold)",
      color: "var(--fos-accent)"
    }
  }, center), /*#__PURE__*/React.createElement("div", {
    style: {
      ...side,
      textAlign: "left"
    }
  }, right));
}
Object.assign(__ds_scope, { SoftKeyBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/SoftKeyBar.jsx", error: String((e && e.message) || e) }); }

// components/navigation/TabRow.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* ViewModeTabRow: 16/6dp outer padding, 8dp gap, 12dp radius, 8dp item padding,
   13sp label. focused = 18% text, idle = 8% text, selected = solid accent. */
function TabRow({
  items = [],
  selected = 0,
  focusedIndex = -1,
  onSelect,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      direction: "rtl",
      fontFamily: "var(--fos-font)",
      display: "flex",
      gap: "var(--fos-space-3)",
      padding: "var(--fos-space-2) var(--fos-space-7)",
      ...style
    }
  }, rest), items.map((label, i) => {
    const sel = i === selected;
    const foc = i === focusedIndex;
    return /*#__PURE__*/React.createElement("div", {
      key: label,
      onClick: () => onSelect && onSelect(i),
      style: {
        flex: 1,
        textAlign: "center",
        fontSize: "var(--fos-size-summary)",
        fontWeight: "var(--fos-weight-medium)",
        padding: "var(--fos-space-3) 0",
        borderRadius: "var(--fos-radius-tab)",
        color: sel ? "var(--fos-on-accent)" : "var(--fos-text)",
        background: sel ? "var(--fos-accent)" : foc ? "var(--fos-focus-bg-chip)" : "var(--fos-text-08)",
        cursor: "pointer",
        transition: "background var(--fos-transition-focus), color var(--fos-transition-focus)"
      }
    }, label);
  }));
}
Object.assign(__ds_scope, { TabRow });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/TabRow.jsx", error: String((e && e.message) || e) }); }

// components/feedback/GlassPanel.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* GlassPanel: a system-shell panel (Control Center, Notification Center) — the one place in
   FutureOS that blurs. It draws a snapshot of the screen behind it blurred at 40dp (80px),
   under a 20% white wash, and lays its children out in a column with 16dp side / 8dp vertical
   padding and 10dp between elements (the Control Center uses 4dp vertical).
   `wallpaper` stands in for the snapshot (any CSS background); without it the panel blurs
   whatever is live behind it with backdrop-filter. App screens never use this. Ink is always
   white, so the panel reads the same in light and dark. */
function GlassPanel({ wallpaper, blur = "var(--fos-backdrop-blur)", children, style, ...rest }) {
  return /*#__PURE__*/React.createElement("div", _extends({
  style: {
          position: "relative",
          isolation: "isolate",
          overflow: "hidden",
          boxSizing: "border-box",
          direction: "rtl",
          fontFamily: "var(--fos-font)",
          color: "var(--fos-shell-ink)",
          ...(wallpaper ? null : { backdropFilter: `blur(${blur})`, WebkitBackdropFilter: `blur(${blur})` }),
          ...style
        }
}, rest), wallpaper && /*#__PURE__*/React.createElement("div", {
  "aria-hidden": "true",
  style: { position: "absolute", inset: `calc(-1 * ${blur})`, zIndex: -2, background: wallpaper, backgroundSize: "cover", backgroundPosition: "center", filter: `blur(${blur})` }
}), /*#__PURE__*/React.createElement("div", {
  "aria-hidden": "true",
  style: { position: "absolute", inset: 0, zIndex: -1, background: "var(--fos-backdrop-wash)" }
}), /*#__PURE__*/React.createElement("div", {
  style: { position: "relative", boxSizing: "border-box", minHeight: "100%", display: "flex", flexDirection: "column", gap: "var(--fos-space-4)", padding: "var(--fos-space-3) var(--fos-space-screen)" }
}, children));
}

/* GlassTile: any element that sits on a GlassPanel — toggle, notification card, media card,
   button. 28dp radius (the shell radius), white at 15% at rest, 24% when focused with a 2dp
   white ring (4px, inside, no scale).
   `on` is the exception: a solid accent fill with on-accent ink, so a toggle reads at any accent.
   Ink is white with a faint legibility shadow; children that draw icons should use currentColor
   so `on` can flip them. */
function GlassTile({ focused = false, on = false, radius = "var(--fos-radius-headsup)", onClick, children, style, ...rest }) {
  return /*#__PURE__*/React.createElement("div", _extends({
  onClick: onClick,
  style: {
          boxSizing: "border-box",
          borderRadius: radius,
          background: on ? "var(--fos-accent)" : focused ? "var(--fos-glass-fill-focused)" : "var(--fos-glass-fill)",
          color: on ? "var(--fos-on-accent)" : "var(--fos-shell-ink)",
          textShadow: on ? "none" : "var(--fos-shell-text-shadow)",
          border: `var(--fos-focus-border-control) solid ${focused ? "var(--fos-shell-ink)" : "transparent"}`,
          cursor: onClick ? "pointer" : undefined,
          transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
          ...style
        }
}, rest), children);
}
Object.assign(__ds_scope, { GlassPanel, GlassTile });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/GlassPanel.jsx", error: String((e && e.message) || e) }); }

// components/feedback/NotificationCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* NotificationCard: one notification in the Notification Center, a GlassTile (white 15% at
   rest, 24% + 2dp white ring when focused) at 28dp radius, 14dp / 13dp padding. Collapsed: a
   38dp app disc (white 20%) and a text column — "app: title" at 14sp/700, then the body at
   13sp in two lines, all of it when `expanded` (long OK). `optionsOpen` (the Options key)
   replaces that content with the notification's actions as OptionsMenu rows stacked
   vertically: 50dp tall, 16dp radius, 15sp, the selected row a 12% white fill with no border,
   a destructive row in the danger-on-glass color; a 12sp app-name header sits above them.
   Always white ink — it sits on a GlassPanel. */
function NotificationCard({ appName, title, body, icon = "chat", focused = false, expanded = false, optionsOpen = false, options = [], focusedOption = 0, onSelect, style, ...rest }) {
  return /*#__PURE__*/React.createElement(__ds_scope.GlassTile, _extends({
  focused: focused,
  radius: "var(--fos-radius-headsup)",
  style: { padding: "26px var(--fos-space-6)", transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus), height var(--fos-transition-focus)", ...style }
}, rest), optionsOpen ? /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
  style: { fontSize: "var(--fos-size-label)", color: "var(--fos-shell-ink-sub)", padding: "0 var(--fos-space-5) var(--fos-space-1)" }
}, appName), options.map((it, i) => /*#__PURE__*/React.createElement("div", {
  key: it.label,
  onClick: () => onSelect && onSelect(i),
  style: {
                  display: "flex",
                  alignItems: "center",
                  height: "var(--fos-row-menu)",
                  padding: "0 var(--fos-space-5)",
                  borderRadius: "var(--fos-radius-card)",
                  background: i === focusedOption ? "var(--fos-shell-ink-12)" : "transparent",
                  color: it.destructive ? "var(--fos-danger-on-glass)" : "var(--fos-shell-ink)",
                  cursor: "pointer",
                  transition: "background var(--fos-transition-focus)"
                }
}, /*#__PURE__*/React.createElement("div", {
  style: { minWidth: 0, fontSize: "var(--fos-size-dialog)", fontWeight: "var(--fos-weight-medium)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }
}, it.label)))) : /*#__PURE__*/React.createElement("div", {
  style: { display: "flex", alignItems: "center", gap: "var(--fos-space-6)" }
}, /*#__PURE__*/React.createElement("div", {
  style: { flex: "0 0 auto", width: 76, height: 76, borderRadius: "var(--fos-radius-full)", background: "var(--fos-shell-ink-20)", display: "grid", placeItems: "center", color: "var(--fos-shell-ink-sub)" }
}, /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
  name: icon,
  size: 36,
  color: "currentColor"
})), /*#__PURE__*/React.createElement("div", {
  style: { flex: 1, minWidth: 0 }
}, /*#__PURE__*/React.createElement("div", {
  style: { fontSize: "var(--fos-size-body)", fontWeight: "var(--fos-weight-bold)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }
}, title ? `${appName}: ${title}` : appName), body && /*#__PURE__*/React.createElement("div", {
  style: {
                    fontSize: "var(--fos-size-summary)",
                    color: "var(--fos-shell-ink-sub)",
                    ...(expanded ? null : { display: "-webkit-box", WebkitBoxOrient: "vertical", WebkitLineClamp: 2, overflow: "hidden" })
                  }
}, body))));
}
Object.assign(__ds_scope, { NotificationCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/NotificationCard.jsx", error: String((e && e.message) || e) }); }

// components/core/Widget.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* Home-screen widget template (FutureContentWidget, 2x1 cells): #1C1C1E at 90% with a 0.5dp 15%
   white hairline, 22dp radius, 16dp / 10dp padding. Row: 18dp app icon, 6dp, 12sp title at 60%
   white. Value 20sp bold white (28sp in the clock variant, a live clock; 2dp tighter above it).
   Subtitle 13sp at 60%, two lines max. The icon is the app's own launcher icon, drawn as is and
   not tinted; `icon` here is a FosIcon stand-in for it. Always dark, even in light mode — it
   sits on the user's wallpaper — hence the shell tokens. */
function Widget({ icon = "widgets", title, value, subtitle, variant = "default", style, ...rest }) {
  const clock = variant === "clock";
  return /*#__PURE__*/React.createElement("div", _extends({
  style: {
          direction: "rtl",
          fontFamily: "var(--fos-font)",
          boxSizing: "border-box",
          padding: "var(--fos-space-4) var(--fos-space-7)",
          borderRadius: "var(--fos-radius-main)",
          background: "var(--fos-widget-bg)",
          border: "var(--fos-border-headsup) solid var(--fos-hairline-dark)",
          color: "var(--fos-shell-ink)",
          ...style
        }
}, rest), /*#__PURE__*/React.createElement("div", {
  style: { display: "flex", alignItems: "center", gap: "var(--fos-space-2)" }
}, /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
  name: icon,
  size: 36,
  color: "var(--fos-shell-ink)"
}), /*#__PURE__*/React.createElement("div", {
  style: { minWidth: 0, fontSize: "var(--fos-size-label)", color: "var(--fos-shell-ink-60)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }
}, title)), /*#__PURE__*/React.createElement("div", {
  style: {
            marginTop: clock ? 4 : "var(--fos-space-1)",
            fontSize: clock ? "var(--fos-size-widget-clock)" : "var(--fos-size-screen-title)",
            fontWeight: "var(--fos-weight-bold)",
            lineHeight: "var(--fos-line-height)",
            fontVariantNumeric: "tabular-nums"
          }
}, value), subtitle && /*#__PURE__*/React.createElement("div", {
  style: {
              fontSize: "var(--fos-size-summary)",
              lineHeight: "var(--fos-line-height)",
              color: "var(--fos-shell-ink-60)",
              display: "-webkit-box",
              WebkitBoxOrient: "vertical",
              WebkitLineClamp: 2,
              overflow: "hidden"
            }
}, subtitle));
}
Object.assign(__ds_scope, { Widget });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Widget.jsx", error: String((e && e.message) || e) }); }

// components/navigation/StatusCapsule.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const PAD = 18; /* 9dp */
const MAX_GLYPHS = 4;

/* StatusCapsule: a status-bar capsule (DynamicCapsule) — #1C1C1E at 55% with a 0.5dp 15% white
   hairline, 22dp (44px) tall, a pill, 9dp inside each end, over a transparent bar. The leading
   capsule is dynamic: it widens (200ms, standard easing, no bounce) to show live activity — a call
   (success fill, black ink, call icon + duration), charging (bolt in success + label), a new
   notification (app glyph + app name, label at most 90dp), media (music note + title, label at
   most 110dp, ellipsized) — then returns to the time and the app glyphs (at most four, then +N).
   Pass children for a static capsule (the trailing device-status capsule). Ink is always white. */
function StatusCapsule({ state = "idle", time, label, apps = [], icon = "chat", children, style, ...rest }) {
  const innerRef = React.useRef(null);
  const [width, setWidth] = React.useState(null);
  React.useLayoutEffect(() => {
    const el = innerRef.current;
    if (!el) return undefined;
    const measure = () => setWidth(Math.ceil(el.getBoundingClientRect().width) + 2);
    measure();
    if (typeof ResizeObserver === "undefined") return undefined;
    const ro = new ResizeObserver(measure);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const call = !children && state === "call";
  const ink = call ? "var(--fos-on-accent)" : "var(--fos-shell-ink)";
  const lead = state === "charging" ? { name: "bolt", size: 24, color: "var(--fos-shell-success)" }
    : state === "notification" ? { name: icon, size: 28, color: ink }
    : state === "media" ? { name: "music_note", size: 28, color: ink }
    : call ? { name: "call", size: 24, color: ink }
    : null;
  const labelMax = state === "notification" ? 180 : state === "media" ? 220 : undefined;
  const shown = apps.slice(0, MAX_GLYPHS);

  let content;
  if (children) content = children;
  else if (state === "idle") {
    content = /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("span", {
  style: { fontSize: "var(--fos-size-body)", fontWeight: "var(--fos-weight-semibold)", fontVariantNumeric: "tabular-nums" }
}, time), shown.length > 0 && /*#__PURE__*/React.createElement("span", {
  style: { display: "flex", alignItems: "center", gap: "var(--fos-space-1)" }
}, shown.map((a, i) => /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
  key: a + i,
  name: a,
  size: 28,
  color: "currentColor"
})), apps.length > MAX_GLYPHS && /*#__PURE__*/React.createElement("span", {
  style: { fontSize: "var(--fos-size-label)", fontWeight: "var(--fos-weight-semibold)", color: "var(--fos-shell-ink-70)" }
}, "+", apps.length - MAX_GLYPHS)));
  } else {
    content = /*#__PURE__*/React.createElement(React.Fragment, null, lead && /*#__PURE__*/React.createElement(__ds_scope.FosIcon, {
  name: lead.name,
  size: lead.size,
  color: lead.color
}), /*#__PURE__*/React.createElement("span", {
  style: {
              maxWidth: labelMax,
              fontSize: "var(--fos-size-label)",
              fontWeight: call ? "var(--fos-weight-bold)" : "var(--fos-weight-medium)",
              fontVariantNumeric: "tabular-nums",
              whiteSpace: "nowrap",
              overflow: "hidden",
              textOverflow: "ellipsis"
            }
}, label));
  }

  return /*#__PURE__*/React.createElement("div", _extends({
  style: {
          direction: "rtl",
          fontFamily: "var(--fos-font)",
          display: "inline-flex",
          alignItems: "center",
          justifyContent: "flex-start",
          height: 44,
          width: width == null ? "max-content" : width,
          boxSizing: "border-box",
          overflow: "hidden",
          borderRadius: "var(--fos-radius-full)",
          background: call ? "var(--fos-shell-success)" : "var(--fos-capsule-bg)",
          border: `var(--fos-border-headsup) solid ${call ? "var(--fos-shell-success)" : "var(--fos-hairline-dark)"}`,
          color: ink,
          transition: "width var(--fos-transition-focus), background var(--fos-transition-focus), border-color var(--fos-transition-focus)",
          ...style
        }
}, rest), /*#__PURE__*/React.createElement("div", {
  ref: innerRef,
  style: {
            flex: "0 0 auto",
            display: "flex",
            alignItems: "center",
            gap: children ? 14 /* 7dp */ : state === "idle" ? "var(--fos-space-2)" : state === "notification" || state === "media" ? 10 /* 5dp */ : "var(--fos-space-1)",
            padding: `0 ${PAD}px`,
            width: "max-content"
          }
}, content));
}
Object.assign(__ds_scope, { StatusCapsule });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/StatusCapsule.jsx", error: String((e && e.message) || e) }); }

// ui_kits/bluetooth/BluetoothScreens.jsx
try { (() => {
/* FutureOS · בלוטות' — screens. Composed from the shipped components.
   Two non-component surfaces: the device hero on the device screen (a large
   glyph + name + status, no component covers it) and the pairing code block
   inside the pairing dialog, which repeats the dialog tokens. */
const {
  TopBar,
  Card,
  Divider,
  SectionHeader,
  ListItem,
  SettingItem,
  EmptyState,
  FosIcon: Icon,
  Switch,
  Button,
  ProgressBar,
  Badge
} = window.FutureOSDesignSystem_3ab611;
const PAIRED = [{
  id: "buds",
  name: "אוזניות",
  type: "אוזניות",
  icon: "headphones",
  battery: 80,
  connected: true
}, {
  id: "car",
  name: "מערכת רכב",
  type: "רכב",
  icon: "directions_car",
  battery: null,
  connected: false
}, {
  id: "speaker",
  name: "רמקול סלון",
  type: "רמקול",
  icon: "speaker",
  battery: 45,
  connected: false
}];
const NEARBY = [{
  id: "watch",
  name: "שעון כושר",
  icon: "watch"
}, {
  id: "laptop",
  name: "מחשב נייד",
  icon: "laptop_mac"
}, {
  id: "phone",
  name: "הטלפון של דנה",
  icon: "smartphone"
}, {
  id: "kbd",
  name: "מקלדת אלחוטית",
  icon: "keyboard"
}];
const MENU = [{
  label: "רענן",
  icon: "refresh"
}, {
  label: "שם המכשיר",
  icon: "edit"
}, {
  label: "קבצים שהתקבלו",
  icon: "folder"
}, {
  label: "הגדרות",
  icon: "settings"
}, {
  label: "נתק הכל",
  icon: "link_off",
  destructive: true
}];
const Chevron = () => /*#__PURE__*/React.createElement(Icon, {
  name: "keyboard_arrow_left",
  size: 36,
  color: "var(--fos-text-30)"
});
const sectionLabel = {
  fontSize: "var(--fos-size-summary)",
  color: "var(--fos-text-55)",
  letterSpacing: "var(--fos-tracking-section)"
};
const DeviceGlyph = ({
  icon,
  color
}) => /*#__PURE__*/React.createElement(Icon, {
  name: icon,
  size: 44,
  color: color || "var(--fos-accent)"
});

/* status line under a paired device name: connected + battery, or plain state */
function statusOf(d) {
  if (d.state === "connecting") return "מתחבר";
  if (d.connected) return d.battery != null ? "מחובר · " + d.battery + "%" : "מחובר";
  return "מותאם";
}
function RootScreen({
  on,
  devices,
  nearby,
  scanning,
  name,
  focus,
  onToggle,
  onName,
  onOpen,
  onPair
}) {
  let i = 1;
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05D1\u05DC\u05D5\u05D8\u05D5\u05EA'",
    onMenu: () => {}
  }), /*#__PURE__*/React.createElement(Card, null, /*#__PURE__*/React.createElement("div", {
    "data-f": "0"
  }, /*#__PURE__*/React.createElement(SettingItem, {
    title: "\u05D1\u05DC\u05D5\u05D8\u05D5\u05EA'",
    summary: on ? "מופעל" : "כבוי",
    icon: "bluetooth",
    trailing: /*#__PURE__*/React.createElement(Switch, {
      on: on,
      onChange: onToggle
    }),
    focused: focus === 0,
    onClick: onToggle
  })), /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement("div", {
    "data-f": "1"
  }, /*#__PURE__*/React.createElement(SettingItem, {
    title: "\u05E9\u05DD \u05D4\u05DE\u05DB\u05E9\u05D9\u05E8",
    summary: name,
    icon: "badge",
    chevron: true,
    focused: focus === 1,
    onClick: onName
  }))), !on ? /*#__PURE__*/React.createElement(EmptyState, {
    icon: "bluetooth_disabled",
    title: "\u05D1\u05DC\u05D5\u05D8\u05D5\u05EA' \u05DB\u05D1\u05D5\u05D9",
    subtitle: "\u05DC\u05D7\u05E5 \u05E2\u05DC \u05D0\u05D9\u05E9\u05D5\u05E8 \u05DB\u05D3\u05D9 \u05DC\u05D4\u05E4\u05E2\u05D9\u05DC \u05D5\u05DC\u05D7\u05E4\u05E9 \u05DE\u05DB\u05E9\u05D9\u05E8\u05D9\u05DD"
  }) : /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(SectionHeader, null, "\u05DE\u05DB\u05E9\u05D9\u05E8\u05D9\u05DD \u05DE\u05D5\u05EA\u05D0\u05DE\u05D9\u05DD"), devices.length === 0 ? /*#__PURE__*/React.createElement(EmptyState, {
    icon: "devices",
    title: "\u05D0\u05D9\u05DF \u05DE\u05DB\u05E9\u05D9\u05E8\u05D9\u05DD \u05DE\u05D5\u05EA\u05D0\u05DE\u05D9\u05DD",
    subtitle: "\u05D1\u05D7\u05E8 \u05DE\u05DB\u05E9\u05D9\u05E8 \u05DE\u05D4\u05E8\u05E9\u05D9\u05DE\u05D4 \u05E9\u05DC\u05DE\u05D8\u05D4 \u05DB\u05D3\u05D9 \u05DC\u05D4\u05EA\u05D0\u05D9\u05DD"
  }) : /*#__PURE__*/React.createElement(Card, null, devices.map((d, n) => {
    const f = ++i;
    return /*#__PURE__*/React.createElement(React.Fragment, {
      key: d.id
    }, n > 0 && /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement("div", {
      "data-f": f
    }, /*#__PURE__*/React.createElement(ListItem, {
      title: d.name,
      summary: statusOf(d),
      focused: focus === f,
      onClick: () => onOpen(d),
      trailing: /*#__PURE__*/React.createElement("div", {
        style: {
          display: "flex",
          alignItems: "center",
          gap: "var(--fos-space-3)"
        }
      }, /*#__PURE__*/React.createElement(DeviceGlyph, {
        icon: d.icon,
        color: d.connected ? "var(--fos-accent)" : "var(--fos-text-40)"
      }), /*#__PURE__*/React.createElement(Chevron, null))
    })), d.state === "connecting" && /*#__PURE__*/React.createElement("div", {
      style: {
        padding: "0 var(--fos-space-6) var(--fos-space-4)"
      }
    }, /*#__PURE__*/React.createElement(ProgressBar, null)));
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      padding: "var(--fos-space-9) var(--fos-space-7) var(--fos-space-3)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: sectionLabel
  }, "\u05DE\u05DB\u05E9\u05D9\u05E8\u05D9\u05DD \u05D6\u05DE\u05D9\u05E0\u05D9\u05DD"), scanning && /*#__PURE__*/React.createElement("div", {
    style: {
      ...sectionLabel,
      color: "var(--fos-text-40)"
    }
  }, "\u05DE\u05D7\u05E4\u05E9")), scanning && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "0 var(--fos-space-7) var(--fos-space-3)"
    }
  }, /*#__PURE__*/React.createElement(ProgressBar, null)), /*#__PURE__*/React.createElement(Card, null, nearby.map((d, n) => {
    const f = ++i;
    return /*#__PURE__*/React.createElement(React.Fragment, {
      key: d.id
    }, n > 0 && /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement("div", {
      "data-f": f
    }, /*#__PURE__*/React.createElement(ListItem, {
      title: d.name,
      focused: focus === f,
      onClick: () => onPair(d),
      trailing: /*#__PURE__*/React.createElement(DeviceGlyph, {
        icon: d.icon,
        color: "var(--fos-text-40)"
      })
    })));
  }))));
}
function DeviceScreen({
  device,
  focus,
  onBack,
  onProfile,
  onConnect,
  onRename,
  onForget
}) {
  const d = device;
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05DE\u05DB\u05E9\u05D9\u05E8",
    onBack: onBack,
    onMenu: () => {}
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: "var(--fos-space-3)",
      padding: "var(--fos-space-5) var(--fos-space-screen) var(--fos-space-9)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 160,
      height: 160,
      borderRadius: 999,
      background: "var(--fos-text-08)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center"
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: d.icon,
    size: 80,
    color: d.connected ? "var(--fos-accent)" : "var(--fos-text-40)"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-screen-title)",
      fontWeight: "var(--fos-weight-bold)",
      textWrap: "pretty",
      textAlign: "center"
    }
  }, d.name), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-body)",
      color: "var(--fos-text-60)"
    }
  }, statusOf(d)), d.state === "connecting" && /*#__PURE__*/React.createElement("div", {
    style: {
      width: "60%",
      paddingTop: "var(--fos-space-2)"
    }
  }, /*#__PURE__*/React.createElement(ProgressBar, null))), /*#__PURE__*/React.createElement(SectionHeader, null, "\u05E9\u05D9\u05DE\u05D5\u05E9"), /*#__PURE__*/React.createElement(Card, null, /*#__PURE__*/React.createElement("div", {
    "data-f": "0"
  }, /*#__PURE__*/React.createElement(SettingItem, {
    title: "\u05E9\u05D9\u05D7\u05D5\u05EA \u05D5\u05D0\u05D5\u05D3\u05D9\u05D5",
    summary: d.calls ? "מופעל" : "כבוי",
    icon: "call",
    trailing: /*#__PURE__*/React.createElement(Switch, {
      on: d.calls,
      onChange: () => onProfile("calls")
    }),
    focused: focus === 0,
    onClick: () => onProfile("calls")
  })), /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement("div", {
    "data-f": "1"
  }, /*#__PURE__*/React.createElement(SettingItem, {
    title: "\u05DE\u05D3\u05D9\u05D4",
    summary: d.media ? "מופעל" : "כבוי",
    icon: "music_note",
    trailing: /*#__PURE__*/React.createElement(Switch, {
      on: d.media,
      onChange: () => onProfile("media")
    }),
    focused: focus === 1,
    onClick: () => onProfile("media")
  })), /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement("div", {
    "data-f": "2"
  }, /*#__PURE__*/React.createElement(SettingItem, {
    title: "\u05E9\u05E0\u05D4 \u05E9\u05DD",
    summary: d.name,
    icon: "edit",
    chevron: true,
    focused: focus === 2,
    onClick: onRename
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: "var(--fos-space-3)",
      padding: "var(--fos-space-9) var(--fos-space-screen) var(--fos-space-7)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    "data-f": "3"
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "secondary",
    fullWidth: true,
    focused: focus === 3,
    onClick: onConnect
  }, d.connected ? "נתק" : "התחבר")), /*#__PURE__*/React.createElement("div", {
    "data-f": "4"
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "quiet",
    fullWidth: true,
    focused: focus === 4,
    onClick: onForget
  }, "\u05E9\u05DB\u05D7 \u05DE\u05DB\u05E9\u05D9\u05E8"))));
}
Object.assign(window, {
  PAIRED,
  NEARBY,
  MENU,
  RootScreen,
  DeviceScreen,
  statusOf
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/bluetooth/BluetoothScreens.jsx", error: String((e && e.message) || e) }); }

// ui_kits/calls/ScreensA.jsx
try { (() => {
const {
  TopBar,
  Card,
  Divider,
  SectionHeader,
  SettingItem,
  ListItem,
  Chip,
  Button,
  FosIcon: Icon,
  TextField,
  EmptyState
} = window.FutureOSDesignSystem_3ab611;
const S = {
  s7: "var(--fos-space-7)",
  s3: "var(--fos-space-3)",
  s5: "var(--fos-space-5)",
  s8: "var(--fos-space-8)",
  s9: "var(--fos-space-9)"
};
const col = gap => ({
  display: "flex",
  flexDirection: "column",
  gap
});
function Avatar({
  name,
  size = 176,
  icon
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      width: size,
      height: size,
      borderRadius: "var(--fos-radius-full)",
      background: "var(--fos-glass)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      flex: "0 0 auto"
    }
  }, icon ? /*#__PURE__*/React.createElement(Icon, {
    name: icon,
    size: Math.round(size * 0.44),
    color: "var(--fos-text-60)"
  }) : /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: Math.round(size * 0.3),
      fontWeight: 500,
      color: "var(--fos-text-70)",
      fontFamily: "var(--fos-font-display)"
    }
  }, window.initials(name)));
}
function ALog({
  focus,
  filter,
  onOpen,
  onMenu
}) {
  const groups = window.CALL_LOG.map(g => ({
    day: g.day,
    items: filter === "missed" ? g.items.filter(i => i.dir === "missed") : g.items
  })).filter(g => g.items.length);
  let i = -1;
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05E9\u05D9\u05D7\u05D5\u05EA",
    onMenu: onMenu
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: `0 ${S.s7}`,
      display: "flex",
      gap: S.s3
    }
  }, /*#__PURE__*/React.createElement(Chip, {
    state: filter === "all" ? "selected" : "idle"
  }, "\u05D4\u05DB\u05DC"), /*#__PURE__*/React.createElement(Chip, {
    state: filter === "missed" ? "selected" : "idle"
  }, "\u05DC\u05D0 \u05E0\u05E2\u05E0\u05D5")), groups.length === 0 ? /*#__PURE__*/React.createElement(EmptyState, {
    icon: "call_missed",
    title: "\u05D0\u05D9\u05DF \u05E9\u05D9\u05D7\u05D5\u05EA \u05E9\u05DC\u05D0 \u05E0\u05E2\u05E0\u05D5",
    subtitle: "\u05DC\u05D7\u05E5 f \u05DB\u05D3\u05D9 \u05DC\u05D7\u05D6\u05D5\u05E8 \u05DC\u05DB\u05DC \u05D4\u05E9\u05D9\u05D7\u05D5\u05EA"
  }) : groups.map(g => /*#__PURE__*/React.createElement("div", {
    key: g.day
  }, /*#__PURE__*/React.createElement(SectionHeader, null, g.day), /*#__PURE__*/React.createElement(Card, null, g.items.map((c, k) => {
    const idx = ++i;
    return /*#__PURE__*/React.createElement(React.Fragment, {
      key: c.name + c.time
    }, k > 0 && /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement("div", {
      "data-f": idx
    }, /*#__PURE__*/React.createElement(SettingItem, {
      title: c.name,
      summary: window.DIR[c.dir].label + (c.dur ? " · " + c.dur : ""),
      icon: window.DIR[c.dir].icon,
      focused: focus === idx,
      chevron: false,
      onClick: () => onOpen(c),
      trailing: /*#__PURE__*/React.createElement("span", {
        style: {
          fontSize: "var(--fos-size-summary)",
          color: "var(--fos-text-40)",
          fontVariantNumeric: "tabular-nums"
        }
      }, c.time)
    })));
  })))));
}
function ADialer({
  digits,
  match,
  onCall
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: "100%",
      display: "flex",
      flexDirection: "column"
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05DE\u05E7\u05DC\u05D3\u05EA"
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: `0 ${S.s7} ${S.s5}`,
      minHeight: 128,
      ...col("6px"),
      justifyContent: "center",
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-header)",
      fontWeight: 300,
      letterSpacing: "2px",
      direction: "ltr",
      color: digits ? "var(--fos-text)" : "var(--fos-text-30)",
      fontFamily: "var(--fos-font-display)"
    }
  }, digits || "הקלד מספר"), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-body)",
      color: "var(--fos-accent)",
      minHeight: 34
    }
  }, match ? match.name : "")), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      padding: `0 ${S.s9}`,
      display: "grid",
      gridTemplateColumns: "repeat(3, 1fr)",
      gap: S.s3,
      alignContent: "center"
    }
  }, window.KEYPAD.map(k => /*#__PURE__*/React.createElement("div", {
    key: k.d,
    style: {
      height: 116,
      borderRadius: "var(--fos-radius-card)",
      background: "var(--fos-calc-button)",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      gap: 2
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--fos-size-screen-title)",
      fontWeight: 500,
      color: "var(--fos-text)"
    }
  }, k.d), k.s && /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--fos-size-badge)",
      color: "var(--fos-text-40)",
      letterSpacing: "1px"
    }
  }, k.s)))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: `${S.s5} ${S.s9} ${S.s7}`
    }
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "primary",
    fullWidth: true,
    focused: !!digits,
    onClick: onCall
  }, "\u05D4\u05EA\u05E7\u05E9\u05E8")));
}
function AList({
  title,
  rows,
  focus,
  empty,
  onOpen,
  onMenu,
  trailing
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: "100%",
      display: "flex",
      flexDirection: "column"
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: title,
    onMenu: onMenu
  }), rows.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement(EmptyState, {
    icon: empty.icon,
    title: empty.title,
    subtitle: empty.subtitle,
    style: {
      height: "100%"
    }
  })) : /*#__PURE__*/React.createElement("div", {
    style: {
      padding: `0 ${S.s7}`,
      ...col("var(--fos-space-item)")
    }
  }, rows.map((r, k) => /*#__PURE__*/React.createElement("div", {
    key: r.name + k,
    "data-f": k
  }, /*#__PURE__*/React.createElement(ListItem, {
    title: r.name,
    summary: r.phone,
    focused: focus === k,
    onClick: () => onOpen(r),
    trailing: trailing ? trailing(r) : null
  })))));
}
function ASearch({
  query,
  rows,
  focus,
  onOpen
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: "100%",
      display: "flex",
      flexDirection: "column"
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05D7\u05D9\u05E4\u05D5\u05E9",
    onBack: () => {}
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: `0 ${S.s7} ${S.s5}`
    }
  }, /*#__PURE__*/React.createElement(TextField, {
    value: query,
    placeholder: "\u05E9\u05DD \u05D0\u05D5 \u05DE\u05E1\u05E4\u05E8",
    focused: true,
    showCaret: true
  })), rows.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement(EmptyState, {
    icon: "search_off",
    title: "\u05D0\u05D9\u05DF \u05EA\u05D5\u05E6\u05D0\u05D5\u05EA",
    subtitle: "\u05E0\u05E1\u05D4 \u05E9\u05DD \u05D0\u05D5 \u05E1\u05E4\u05E8\u05D5\u05EA \u05D0\u05D7\u05E8\u05D5\u05EA",
    style: {
      height: "100%"
    }
  })) : /*#__PURE__*/React.createElement("div", {
    style: {
      padding: `0 ${S.s7}`,
      ...col("var(--fos-space-item)")
    }
  }, rows.map((r, k) => /*#__PURE__*/React.createElement("div", {
    key: r.name,
    "data-f": k
  }, /*#__PURE__*/React.createElement(ListItem, {
    title: r.name,
    summary: r.phone,
    focused: focus === k,
    onClick: () => onOpen(r)
  })))));
}
function AContact({
  contact,
  focus,
  onCall,
  onBack,
  onMenu
}) {
  const history = window.flatLog().filter(c => c.phone === contact.phone).slice(0, 3);
  const actions = [{
    title: "התקשר",
    icon: "call"
  }, {
    title: "שלח הודעה",
    icon: "chat"
  }, {
    title: "הוסף למועדפים",
    icon: "star"
  }];
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05D0\u05D9\u05E9 \u05E7\u05E9\u05E8",
    onBack: onBack,
    onMenu: onMenu
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      ...col("var(--fos-space-3)"),
      alignItems: "center",
      padding: `${S.s5} 0 ${S.s8}`
    }
  }, /*#__PURE__*/React.createElement(Avatar, {
    name: contact.name
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-screen-title)",
      fontWeight: 700,
      fontFamily: "var(--fos-font-display)"
    }
  }, contact.name), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-body)",
      color: "var(--fos-text-60)",
      direction: "ltr",
      fontVariantNumeric: "tabular-nums"
    }
  }, contact.phone)), /*#__PURE__*/React.createElement(Card, null, actions.map((a, k) => /*#__PURE__*/React.createElement(React.Fragment, {
    key: a.title
  }, k > 0 && /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement("div", {
    "data-f": k
  }, /*#__PURE__*/React.createElement(SettingItem, {
    title: a.title,
    icon: a.icon,
    chevron: false,
    focused: focus === k,
    onClick: k === 0 ? onCall : undefined
  }))))), history.length > 0 && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(SectionHeader, null, "\u05D4\u05D9\u05E1\u05D8\u05D5\u05E8\u05D9\u05D4"), /*#__PURE__*/React.createElement(Card, null, history.map((c, k) => /*#__PURE__*/React.createElement(React.Fragment, {
    key: c.time + k
  }, k > 0 && /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement(SettingItem, {
    title: window.DIR[c.dir].label,
    summary: c.dur ? c.dur : "—",
    icon: window.DIR[c.dir].icon,
    chevron: false,
    trailing: /*#__PURE__*/React.createElement("span", {
      style: {
        fontSize: "var(--fos-size-summary)",
        color: "var(--fos-text-40)"
      }
    }, c.time)
  }))))));
}
function AIncoming({
  contact,
  focus
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: "100%",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "space-between",
      padding: `${S.s9} ${S.s9}`
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      ...col("var(--fos-space-3)"),
      alignItems: "center",
      marginTop: 40
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-label)",
      letterSpacing: "var(--fos-tracking-section)",
      color: "var(--fos-text-60)"
    }
  }, "\u05E9\u05D9\u05D7\u05D4 \u05E0\u05DB\u05E0\u05E1\u05EA"), /*#__PURE__*/React.createElement(Avatar, {
    name: contact.name,
    size: 200
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-header)",
      fontWeight: 700,
      fontFamily: "var(--fos-font-display)",
      textAlign: "center"
    }
  }, contact.name), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-body)",
      color: "var(--fos-text-60)",
      direction: "ltr"
    }
  }, contact.phone)), /*#__PURE__*/React.createElement("div", {
    style: {
      width: "100%",
      ...col("var(--fos-space-3)")
    }
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "primary",
    fullWidth: true,
    focused: focus === 0
  }, "\u05E2\u05E0\u05D4"), /*#__PURE__*/React.createElement(Button, {
    variant: "destructive",
    fullWidth: true,
    focused: focus === 1
  }, "\u05D3\u05D7\u05D4")));
}
function AActive({
  contact,
  elapsed,
  focus,
  mute,
  speaker,
  hold
}) {
  const controls = [{
    title: mute ? "בטל השתקה" : "השתק",
    icon: mute ? "mic_off" : "mic",
    on: mute
  }, {
    title: "רמקול",
    icon: "volume_up",
    on: speaker
  }, {
    title: hold ? "המשך" : "המתנה",
    icon: hold ? "play_arrow" : "pause",
    on: hold
  }, {
    title: "מקלדת",
    icon: "dialpad"
  }];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: "100%",
      display: "flex",
      flexDirection: "column"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      ...col("var(--fos-space-2)"),
      alignItems: "center",
      padding: `${S.s9} 0 ${S.s8}`
    }
  }, /*#__PURE__*/React.createElement(Avatar, {
    name: contact.name,
    size: 160
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-screen-title)",
      fontWeight: 700,
      fontFamily: "var(--fos-font-display)"
    }
  }, contact.name), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-body)",
      color: "var(--fos-success)",
      fontVariantNumeric: "tabular-nums",
      direction: "ltr"
    }
  }, elapsed)), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      padding: `0 ${S.s9}`,
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: S.s3,
      alignContent: "start"
    }
  }, controls.map((c, k) => /*#__PURE__*/React.createElement("div", {
    key: c.title,
    "data-f": k,
    style: {
      height: 132,
      borderRadius: "var(--fos-radius-card)",
      background: c.on ? "var(--fos-accent-20)" : "var(--fos-glass)",
      border: focus === k ? "2px solid var(--fos-accent)" : "2px solid transparent",
      ...col("6px"),
      alignItems: "center",
      justifyContent: "center"
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: c.icon,
    size: 40,
    color: c.on ? "var(--fos-accent)" : "var(--fos-text-70)"
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--fos-size-summary)",
      color: "var(--fos-text-60)"
    }
  }, c.title)))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: `${S.s5} ${S.s9} ${S.s8}`
    }
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "destructive",
    fullWidth: true,
    focused: focus === 4
  }, "\u05E1\u05D9\u05D9\u05DD \u05E9\u05D9\u05D7\u05D4")));
}
function AEnded({
  contact,
  duration,
  focus
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: "100%",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      gap: S.s5,
      padding: S.s9
    }
  }, /*#__PURE__*/React.createElement(Avatar, {
    name: contact.name,
    size: 160
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      ...col("6px"),
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-screen-title)",
      fontWeight: 700,
      fontFamily: "var(--fos-font-display)"
    }
  }, contact.name), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-body)",
      color: "var(--fos-text-60)"
    }
  }, "\u05D4\u05E9\u05D9\u05D7\u05D4 \u05D4\u05E1\u05EA\u05D9\u05D9\u05DE\u05D4 \xB7 ", /*#__PURE__*/React.createElement("span", {
    style: {
      direction: "ltr",
      display: "inline-block"
    }
  }, duration))), /*#__PURE__*/React.createElement("div", {
    style: {
      width: "100%",
      ...col("var(--fos-space-3)"),
      marginTop: S.s5
    }
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "primary",
    fullWidth: true,
    focused: focus === 0
  }, "\u05D4\u05EA\u05E7\u05E9\u05E8 \u05E9\u05D5\u05D1"), /*#__PURE__*/React.createElement(Button, {
    variant: "quiet",
    fullWidth: true,
    focused: focus === 1
  }, "\u05D7\u05D6\u05D5\u05E8 \u05DC\u05D9\u05D5\u05DE\u05DF")));
}
Object.assign(window, {
  ALog,
  ADialer,
  AList,
  ASearch,
  AContact,
  AIncoming,
  AActive,
  AEnded
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/calls/ScreensA.jsx", error: String((e && e.message) || e) }); }

// ui_kits/calls/data.js
try { (() => {
/* Sample data for both calls variations. Plain script — no JSX. */
const CALL_LOG = [{
  day: "היום",
  items: [{
    name: "מיכל לוי",
    phone: "052-3334455",
    dir: "in",
    time: "14:02",
    dur: "4:12"
  }, {
    name: "052-3334455",
    phone: "052-3334455",
    dir: "missed",
    time: "12:40"
  }, {
    name: "דני כהן",
    phone: "054-7778899",
    dir: "out",
    time: "11:15",
    dur: "0:48"
  }]
}, {
  day: "אתמול",
  items: [{
    name: "שירה דהן",
    phone: "052-9991122",
    dir: "in",
    time: "19:30",
    dur: "12:03"
  }, {
    name: "אבי מזרחי",
    phone: "050-1112233",
    dir: "missed",
    time: "17:08"
  }, {
    name: "נועה ברק",
    phone: "053-9998877",
    dir: "out",
    time: "09:22",
    dur: "2:31"
  }]
}];
const FAVORITES = [{
  name: "מיכל לוי",
  phone: "052-3334455"
}, {
  name: "אבי מזרחי",
  phone: "050-1112233"
}, {
  name: "אמא",
  phone: "050-4445566"
}, {
  name: "דני כהן",
  phone: "054-7778899"
}];
const CONTACTS_ALL = [{
  name: "אבי מזרחי",
  phone: "050-1112233"
}, {
  name: "אמא",
  phone: "050-4445566"
}, {
  name: "דני כהן",
  phone: "054-7778899"
}, {
  name: "מיכל לוי",
  phone: "052-3334455"
}, {
  name: "נועה ברק",
  phone: "053-9998877"
}, {
  name: "שירה דהן",
  phone: "052-9991122"
}];
const DIR = {
  in: {
    icon: "call_received",
    label: "נכנסת"
  },
  out: {
    icon: "call_made",
    label: "יוצאת"
  },
  missed: {
    icon: "call_missed",
    label: "לא נענתה"
  }
};
const KEYPAD = [{
  d: "1",
  s: ""
}, {
  d: "2",
  s: "ABC"
}, {
  d: "3",
  s: "DEF"
}, {
  d: "4",
  s: "GHI"
}, {
  d: "5",
  s: "JKL"
}, {
  d: "6",
  s: "MNO"
}, {
  d: "7",
  s: "PQRS"
}, {
  d: "8",
  s: "TUV"
}, {
  d: "9",
  s: "WXYZ"
}, {
  d: "*",
  s: ""
}, {
  d: "0",
  s: "+"
}, {
  d: "#",
  s: ""
}];
const flatLog = () => CALL_LOG.flatMap(g => g.items);
const initials = n => /[A-Za-z\u0590-\u05FF]/.test(n[0]) ? n.trim().split(" ").slice(0, 2).map(w => w[0]).join("") : "#";
Object.assign(window, {
  CALL_LOG,
  FAVORITES,
  CONTACTS_ALL,
  DIR,
  KEYPAD,
  flatLog,
  initials
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/calls/data.js", error: String((e && e.message) || e) }); }

// ui_kits/clock/ClockScreens.jsx
try { (() => {
const {
  TopBar,
  Card,
  Divider,
  SettingItem,
  Switch,
  EmptyState,
  ProgressBar
} = window.FutureOSDesignSystem_3ab611;
const ALARMS = [{
  time: "07:00",
  days: "ימי חול",
  on: true
}, {
  time: "08:30",
  days: "שבת",
  on: false
}, {
  time: "13:15",
  days: "חד פעמי",
  on: true
}];
const ZONES = [{
  city: "ירושלים",
  zone: "עכשיו",
  time: "07:24"
}, {
  city: "לונדון",
  zone: "אתמול · מינוס 2ש׳",
  time: "05:24"
}, {
  city: "ניו יורק",
  zone: "אתמול · מינוס 7ש׳",
  time: "00:24"
}, {
  city: "בנגקוק",
  zone: "פלוס 4ש׳",
  time: "11:24"
}];
function ClockScreen() {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: "100%",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      gap: "var(--fos-space-3)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontFamily: "var(--fos-font-mono)",
      fontSize: "var(--fos-size-clock)",
      fontWeight: "var(--fos-weight-light)",
      lineHeight: 1
    }
  }, "07:24"), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-base)",
      color: "var(--fos-text-60)"
    }
  }, "\u05D9\u05D5\u05DD \u05E9\u05E0\u05D9, 13 \u05D1\u05E1\u05E4\u05D8\u05DE\u05D1\u05E8"));
}
function AlarmsScreen({
  focus,
  alarms,
  onToggle,
  onEdit
}) {
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05DE\u05E2\u05D5\u05E8\u05E8",
    onMenu: () => {}
  }), /*#__PURE__*/React.createElement(Card, null, alarms.map((a, i) => /*#__PURE__*/React.createElement("div", {
    key: a.time
  }, i > 0 && /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement(SettingItem, {
    title: a.time,
    summary: a.days,
    icon: "alarm",
    focused: focus === i,
    trailing: /*#__PURE__*/React.createElement(Switch, {
      on: a.on
    }),
    onClick: () => onEdit(i)
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "var(--fos-space-8) var(--fos-space-9)",
      fontSize: "var(--fos-size-summary)",
      color: "var(--fos-text-40)"
    }
  }, "\u05D0\u05D9\u05E9\u05D5\u05E8 \u05E4\u05D5\u05EA\u05D7 \u05D0\u05EA \u05D1\u05D5\u05E8\u05E8 \u05D4\u05E9\u05E2\u05D4. \u05DE\u05E7\u05E9 \u05D4\u05EA\u05E4\u05E8\u05D9\u05D8 \u05DE\u05D5\u05E1\u05D9\u05E3 \u05DE\u05E2\u05D5\u05E8\u05E8 \u05D7\u05D3\u05E9."));
}
function StopwatchScreen({
  running,
  value
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: "100%",
      display: "flex",
      flexDirection: "column"
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05E1\u05D8\u05D5\u05E4\u05E8"
  }), running ? /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      gap: "var(--fos-space-8)",
      padding: "0 var(--fos-space-7)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontFamily: "var(--fos-font-mono)",
      fontSize: "var(--fos-size-clock)",
      fontWeight: "var(--fos-weight-light)",
      lineHeight: 1
    }
  }, "00:42"), /*#__PURE__*/React.createElement("div", {
    style: {
      width: "100%"
    }
  }, /*#__PURE__*/React.createElement(ProgressBar, {
    value: value
  }))) : /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement(EmptyState, {
    icon: "timer",
    title: "\u05D4\u05E1\u05D8\u05D5\u05E4\u05E8 \u05E2\u05E6\u05D5\u05E8",
    subtitle: "\u05DC\u05D7\u05E5 \u05E2\u05DC \u05D0\u05D9\u05E9\u05D5\u05E8 \u05DB\u05D3\u05D9 \u05DC\u05D4\u05EA\u05D7\u05D9\u05DC",
    style: {
      height: "100%"
    }
  })));
}
function WorldClockScreen({
  focus
}) {
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05E9\u05E2\u05D5\u05DF \u05E2\u05D5\u05DC\u05DE\u05D9",
    onMenu: () => {}
  }), /*#__PURE__*/React.createElement(Card, null, ZONES.map((z, i) => /*#__PURE__*/React.createElement("div", {
    key: z.city,
    "data-f": i
  }, i > 0 && /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement(SettingItem, {
    title: z.city,
    summary: z.zone,
    icon: "public",
    chevron: false,
    focused: focus === i,
    trailing: /*#__PURE__*/React.createElement("span", {
      style: {
        fontFamily: "var(--fos-font-mono)",
        fontSize: "var(--fos-size-title)",
        color: "var(--fos-text)"
      }
    }, z.time)
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "var(--fos-space-8) var(--fos-space-9)",
      fontSize: "var(--fos-size-summary)",
      color: "var(--fos-text-40)"
    }
  }, "\u05DE\u05E7\u05E9 \u05D4\u05EA\u05E4\u05E8\u05D9\u05D8 \u05DE\u05D5\u05E1\u05D9\u05E3 \u05E2\u05D9\u05E8."));
}
function TimerScreen({
  running,
  value
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: "100%",
      display: "flex",
      flexDirection: "column"
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05D8\u05D9\u05D9\u05DE\u05E8",
    onMenu: () => {}
  }), running ? /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      gap: "var(--fos-space-8)",
      padding: "0 var(--fos-space-7)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontFamily: "var(--fos-font-mono)",
      fontSize: "var(--fos-size-clock)",
      fontWeight: "var(--fos-weight-light)",
      lineHeight: 1
    }
  }, "04:35"), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-body)",
      color: "var(--fos-text-60)"
    }
  }, "\u05DE\u05EA\u05D5\u05DA 5:00 \xB7 \u05D1\u05D9\u05E9\u05D5\u05DC"), /*#__PURE__*/React.createElement("div", {
    style: {
      width: "100%"
    }
  }, /*#__PURE__*/React.createElement(ProgressBar, {
    value: value
  }))) : /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement(EmptyState, {
    icon: "hourglass_empty",
    title: "\u05D0\u05D9\u05DF \u05D8\u05D9\u05D9\u05DE\u05E8 \u05E4\u05E2\u05D9\u05DC",
    subtitle: "\u05DC\u05D7\u05E5 \u05E2\u05DC \u05D0\u05D9\u05E9\u05D5\u05E8 \u05DB\u05D3\u05D9 \u05DC\u05D4\u05D2\u05D3\u05D9\u05E8 \u05DE\u05E9\u05DA",
    style: {
      height: "100%"
    }
  })));
}
Object.assign(window, {
  ClockScreen,
  AlarmsScreen,
  StopwatchScreen,
  WorldClockScreen,
  TimerScreen,
  ALARMS,
  ZONES
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/clock/ClockScreens.jsx", error: String((e && e.message) || e) }); }

// ui_kits/communication/CommScreens.jsx
try { (() => {
const {
  TopBar,
  ListItem,
  EmptyState,
  FosIcon: Icon,
  Badge
} = window.FutureOSDesignSystem_3ab611;
const RECENTS = [{
  name: "מיכל לוי",
  meta: "שיחה נכנסת · 14:02",
  icon: "call_received"
}, {
  name: "052-3334455",
  meta: "שיחה שלא נענתה · 12:40",
  icon: "call_missed",
  missed: true
}, {
  name: "דני כהן",
  meta: "שיחה יוצאת · אתמול",
  icon: "call_made"
}, {
  name: "שירה דהן",
  meta: "שיחה נכנסת · אתמול",
  icon: "call_received"
}, {
  name: "אבי מזרחי",
  meta: "שיחה יוצאת · יום ראשון",
  icon: "call_made"
}];
const CONTACTS = [{
  name: "אבי מזרחי",
  phone: "050-1112233",
  fav: true
}, {
  name: "דני כהן",
  phone: "054-7778899"
}, {
  name: "מיכל לוי",
  phone: "052-3334455",
  fav: true
}, {
  name: "נועה ברק",
  phone: "053-9998877"
}, {
  name: "שירה דהן",
  phone: "052-3334455"
}];
const THREADS = [{
  name: "מיכל לוי",
  last: "נתראה בערב",
  unread: 2
}, {
  name: "דני כהן",
  last: "שלחתי לך את הכתובת",
  unread: 0
}, {
  name: "בזק",
  last: "החשבון שלך זמין לצפייה",
  unread: 1
}, {
  name: "נועה ברק",
  last: "תודה רבה",
  unread: 0
}];
function RecentsScreen({
  focus,
  onMenu
}) {
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05E9\u05D9\u05D7\u05D5\u05EA",
    onMenu: onMenu
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "0 var(--fos-space-7)",
      display: "flex",
      flexDirection: "column",
      gap: "var(--fos-space-item)"
    }
  }, RECENTS.map((r, i) => /*#__PURE__*/React.createElement(ListItem, {
    key: r.name + i,
    title: r.name,
    summary: r.meta,
    focused: focus === i,
    trailing: /*#__PURE__*/React.createElement(Icon, {
      name: r.icon,
      size: 36,
      color: r.missed ? "var(--fos-danger)" : "var(--fos-text-40)"
    })
  }))));
}
function ContactsScreen({
  focus,
  empty,
  onMenu
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: "100%",
      display: "flex",
      flexDirection: "column"
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05D0\u05E0\u05E9\u05D9 \u05E7\u05E9\u05E8",
    onMenu: onMenu
  }), empty ? /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement(EmptyState, {
    icon: "person",
    title: "\u05D0\u05D9\u05DF \u05D0\u05E0\u05E9\u05D9 \u05E7\u05E9\u05E8",
    subtitle: "\u05DC\u05D7\u05E5 \u05E2\u05DC \u05DE\u05E7\u05E9 \u05D4\u05EA\u05E4\u05E8\u05D9\u05D8 \u05DB\u05D3\u05D9 \u05DC\u05D4\u05D5\u05E1\u05D9\u05E3",
    style: {
      height: "100%"
    }
  })) : /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "0 var(--fos-space-7)",
      display: "flex",
      flexDirection: "column",
      gap: "var(--fos-space-item)"
    }
  }, CONTACTS.map((c, i) => /*#__PURE__*/React.createElement(ListItem, {
    key: c.name,
    title: c.name,
    summary: c.phone,
    focused: focus === i,
    trailing: c.fav ? /*#__PURE__*/React.createElement(Icon, {
      name: "star",
      size: 36,
      fill: 1,
      color: "var(--fos-favorite)"
    }) : null
  }))));
}
function MessagesScreen({
  focus,
  onMenu
}) {
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05D4\u05D5\u05D3\u05E2\u05D5\u05EA",
    onMenu: onMenu
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "0 var(--fos-space-7)",
      display: "flex",
      flexDirection: "column",
      gap: "var(--fos-space-item)"
    }
  }, THREADS.map((t, i) => /*#__PURE__*/React.createElement(ListItem, {
    key: t.name,
    title: t.name,
    summary: t.last,
    focused: focus === i,
    trailing: t.unread ? /*#__PURE__*/React.createElement(Badge, {
      count: t.unread
    }) : null
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "var(--fos-space-8) var(--fos-space-9)",
      fontSize: "var(--fos-size-summary)",
      color: "var(--fos-text-30)",
      lineHeight: 1.4
    }
  }, "\u05EA\u05E6\u05D5\u05D2\u05EA \u05E9\u05E8\u05E9\u05D5\u05E8 \u05D4\u05D4\u05D5\u05D3\u05E2\u05D5\u05EA \u05E2\u05E6\u05DE\u05D5 \u05DC\u05D0 \u05DE\u05D5\u05E4\u05D9\u05E2\u05D4 \u05D1\u05DE\u05E7\u05D5\u05E8\u05D5\u05EA \u05D5\u05DC\u05DB\u05DF \u05D4\u05D5\u05E9\u05D0\u05E8\u05D4 \u05D1\u05DB\u05D5\u05D5\u05E0\u05D4 \u05DE\u05D7\u05D5\u05E5 \u05DC\u05E2\u05E8\u05DB\u05D4."));
}
Object.assign(window, {
  RecentsScreen,
  ContactsScreen,
  MessagesScreen,
  RECENTS,
  CONTACTS,
  THREADS
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/communication/CommScreens.jsx", error: String((e && e.message) || e) }); }

// ui_kits/settings/SettingsScreens.jsx
try { (() => {
const {
  TopBar,
  Card,
  SectionHeader,
  Divider,
  SettingItem,
  Switch,
  Slider,
  FosIcon: Icon,
  ConfirmDialog,
  OptionsMenu,
  Chip
} = window.FutureOSDesignSystem_3ab611;
const GROUPS = [{
  header: "כללי",
  rows: [{
    id: "display",
    title: "תצוגה",
    summary: "בהירות, גודל טקסט",
    icon: "brightness_6"
  }, {
    id: "sound",
    title: "צלילים",
    summary: "עוצמת מדיה ורינגטון",
    icon: "volume_up"
  }, {
    id: "accent",
    title: "צבע הדגשה",
    summary: "לבן",
    icon: "palette"
  }]
}, {
  header: "מערכת",
  rows: [{
    id: "language",
    title: "שפה",
    summary: "עברית",
    icon: "language"
  }, {
    id: "keys",
    title: "מקשים",
    summary: "קיצורי מקשים מספריים",
    icon: "keyboard"
  }, {
    id: "about",
    title: "אודות",
    summary: "FutureOS 1.0",
    icon: "info"
  }]
}];
function SettingsRoot({
  focus,
  onOpen,
  accentName
}) {
  let i = -1;
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05D4\u05D2\u05D3\u05E8\u05D5\u05EA",
    onBack: () => {},
    onMenu: () => {}
  }), GROUPS.map(g => /*#__PURE__*/React.createElement("div", {
    key: g.header
  }, /*#__PURE__*/React.createElement(SectionHeader, null, g.header), /*#__PURE__*/React.createElement(Card, null, g.rows.map((r, k) => {
    i++;
    const idx = i;
    return /*#__PURE__*/React.createElement("div", {
      key: r.id,
      "data-f": idx
    }, k > 0 && /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement(SettingItem, {
      title: r.title,
      summary: r.id === "accent" ? accentName : r.summary,
      icon: r.icon,
      focused: focus === idx,
      onClick: () => onOpen(r.id, idx)
    }));
  })))));
}
function DisplayScreen({
  focus,
  values,
  onBack,
  onToggle
}) {
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05EA\u05E6\u05D5\u05D2\u05D4",
    onBack: onBack,
    onMenu: () => {}
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: "var(--fos-space-2)",
      display: "flex",
      flexDirection: "column",
      gap: "var(--fos-space-5)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    "data-f": "0"
  }, /*#__PURE__*/React.createElement(Slider, {
    label: "\u05D1\u05D4\u05D9\u05E8\u05D5\u05EA \u05DE\u05E1\u05DA",
    value: values.brightness,
    focused: focus === 0
  })), /*#__PURE__*/React.createElement("div", {
    "data-f": "1"
  }, /*#__PURE__*/React.createElement(Slider, {
    label: "\u05D2\u05D5\u05D3\u05DC \u05D8\u05E7\u05E1\u05D8",
    value: values.textSize,
    focused: focus === 1
  }))), /*#__PURE__*/React.createElement(SectionHeader, null, "\u05EA\u05E6\u05D5\u05D2\u05D4"), /*#__PURE__*/React.createElement(Card, null, /*#__PURE__*/React.createElement("div", {
    "data-f": "2"
  }, /*#__PURE__*/React.createElement(SettingItem, {
    title: "\u05DE\u05E6\u05D1 \u05DB\u05D4\u05D4",
    summary: values.dark ? "מופעל" : "כבוי",
    icon: "dark_mode",
    focused: focus === 2,
    trailing: /*#__PURE__*/React.createElement(Switch, {
      on: values.dark
    }),
    onClick: () => onToggle("dark")
  })), /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement("div", {
    "data-f": "3"
  }, /*#__PURE__*/React.createElement(SettingItem, {
    title: "\u05D1\u05D4\u05D9\u05E8\u05D5\u05EA \u05D0\u05D3\u05E4\u05D8\u05D9\u05D1\u05D9\u05EA",
    summary: "\u05D4\u05EA\u05D0\u05DE\u05D4 \u05DC\u05EA\u05D0\u05D5\u05E8\u05EA \u05D4\u05E1\u05D1\u05D9\u05D1\u05D4",
    icon: "brightness_auto",
    focused: focus === 3,
    trailing: /*#__PURE__*/React.createElement(Switch, {
      on: values.adaptive
    }),
    onClick: () => onToggle("adaptive")
  })), /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement("div", {
    "data-f": "4"
  }, /*#__PURE__*/React.createElement(SettingItem, {
    title: "\u05D0\u05E0\u05D9\u05DE\u05E6\u05D9\u05D5\u05EA",
    summary: values.anim ? "מופעל" : "כבוי",
    icon: "animation",
    focused: focus === 4,
    trailing: /*#__PURE__*/React.createElement(Switch, {
      on: values.anim
    }),
    onClick: () => onToggle("anim")
  }))));
}
const ACCENTS = [{
  name: "לבן",
  value: "#FFFFFF"
}, {
  name: "תכלת",
  value: "#64D2FF"
}, {
  name: "כתום",
  value: "#FF9F0A"
}, {
  name: "ירוק",
  value: "#30D158"
}, {
  name: "סגול",
  value: "#BF5AF2"
}];
function AccentScreen({
  focus,
  selected,
  onBack,
  onPick
}) {
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05E6\u05D1\u05E2 \u05D4\u05D3\u05D2\u05E9\u05D4",
    onBack: onBack
  }), /*#__PURE__*/React.createElement(Card, null, ACCENTS.map((a, i) => /*#__PURE__*/React.createElement("div", {
    key: a.name
  }, i > 0 && /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement(SettingItem, {
    title: a.name,
    icon: "circle",
    chevron: false,
    focused: focus === i,
    onClick: () => onPick(i),
    trailing: selected === i ? /*#__PURE__*/React.createElement(Icon, {
      name: "check",
      size: 44,
      color: "var(--fos-accent)"
    }) : /*#__PURE__*/React.createElement("div", {
      style: {
        width: 44
      }
    }),
    style: {
      "--fos-accent": a.value
    }
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "var(--fos-space-8) var(--fos-space-9)",
      fontSize: "var(--fos-size-summary)",
      color: "var(--fos-text-40)",
      lineHeight: 1.4
    }
  }, "\u05E6\u05D1\u05E2 \u05D4\u05D4\u05D3\u05D2\u05E9\u05D4 \u05D4\u05D5\u05D0 \u05D4\u05E6\u05D1\u05E2 \u05D4\u05D9\u05D7\u05D9\u05D3 \u05E9\u05D4\u05DE\u05E9\u05EA\u05DE\u05E9 \u05D1\u05D5\u05D7\u05E8. \u05D4\u05D5\u05D0 \u05DE\u05E1\u05DE\u05DF \u05E4\u05D5\u05E7\u05D5\u05E1 \u05D5\u05D1\u05D7\u05D9\u05E8\u05D4 \u05D1\u05DC\u05D1\u05D3."));
}
function SoundScreen({
  focus,
  values,
  onBack
}) {
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05E6\u05DC\u05D9\u05DC\u05D9\u05DD",
    onBack: onBack
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: "var(--fos-space-2)",
      display: "flex",
      flexDirection: "column",
      gap: "var(--fos-space-5)"
    }
  }, /*#__PURE__*/React.createElement(Slider, {
    label: "\u05E2\u05D5\u05E6\u05DE\u05EA \u05DE\u05D3\u05D9\u05D4",
    value: values.media,
    focused: focus === 0
  }), /*#__PURE__*/React.createElement(Slider, {
    label: "\u05E2\u05D5\u05E6\u05DE\u05EA \u05E8\u05D9\u05E0\u05D2\u05D8\u05D5\u05DF",
    value: values.ring,
    focused: focus === 1
  }), /*#__PURE__*/React.createElement(Slider, {
    label: "\u05E2\u05D5\u05E6\u05DE\u05EA \u05D4\u05EA\u05E8\u05D0\u05D5\u05EA",
    value: values.notif,
    focused: focus === 2
  })));
}
function AboutScreen({
  focus,
  onBack,
  onReset
}) {
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05D0\u05D5\u05D3\u05D5\u05EA",
    onBack: onBack
  }), /*#__PURE__*/React.createElement(Card, null, /*#__PURE__*/React.createElement(SettingItem, {
    title: "\u05D2\u05E8\u05E1\u05D4",
    summary: "FutureOS 1.0",
    icon: "info",
    chevron: false,
    focused: focus === 0
  }), /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement(SettingItem, {
    title: "\u05DE\u05E1\u05DA",
    summary: "640 \xD7 960 \xB7 \u05E6\u05E4\u05D9\u05E4\u05D5\u05EA 2.0",
    icon: "smartphone",
    chevron: false,
    focused: focus === 1
  }), /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement(SettingItem, {
    title: "\u05D0\u05D9\u05E4\u05D5\u05E1 \u05D4\u05D2\u05D3\u05E8\u05D5\u05EA",
    icon: "restart_alt",
    focused: focus === 2,
    onClick: onReset
  })));
}
Object.assign(window, {
  SettingsRoot,
  DisplayScreen,
  AccentScreen,
  SoundScreen,
  AboutScreen,
  ACCENTS,
  GROUPS
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/settings/SettingsScreens.jsx", error: String((e && e.message) || e) }); }

// ui_kits/translate/TranslateScreens.jsx
try { (() => {
/* FutureOS · תרגום — screens. Composed from the shipped components only.
   The one non-component surface is the translate input box: TextField is
   single-line by design, and translation input has to wrap, so the box repeats
   the field tokens verbatim. */
const {
  TopBar,
  Card,
  Divider,
  SectionHeader,
  ListItem,
  EmptyState,
  FosIcon: Icon,
  IconButton,
  TabRow,
  ProgressBar,
  TextField
} = window.FutureOSDesignSystem_3ab611;
const LANGS = [{
  code: "auto",
  name: "זיהוי שפה",
  rtl: true
}, {
  code: "he",
  name: "עברית",
  rtl: true
}, {
  code: "en",
  name: "אנגלית",
  rtl: false
}, {
  code: "ar",
  name: "ערבית",
  rtl: true
}, {
  code: "ru",
  name: "רוסית",
  rtl: false
}, {
  code: "fr",
  name: "צרפתית",
  rtl: false
}, {
  code: "es",
  name: "ספרדית",
  rtl: false
}, {
  code: "de",
  name: "גרמנית",
  rtl: false
}, {
  code: "it",
  name: "איטלקית",
  rtl: false
}, {
  code: "am",
  name: "אמהרית",
  rtl: false
}, {
  code: "yi",
  name: "יידיש",
  rtl: true
}, {
  code: "tr",
  name: "טורקית",
  rtl: false
}, {
  code: "zh",
  name: "סינית",
  rtl: false
}, {
  code: "ja",
  name: "יפנית",
  rtl: false
}];
const RECENT_LANGS = ["en", "ar", "ru"];
const byCode = c => LANGS.find(l => l.code === c) || LANGS[1];
const HISTORY = [{
  src: "איפה תחנת הרכבת?",
  dst: "Where is the train station?",
  from: "he",
  to: "en",
  saved: true
}, {
  src: "כמה זה עולה",
  dst: "How much does it cost",
  from: "he",
  to: "en",
  saved: false
}, {
  src: "شكرا جزيلا",
  dst: "תודה רבה",
  from: "ar",
  to: "he",
  saved: true
}, {
  src: "אני צריך רופא",
  dst: "J'ai besoin d'un médecin",
  from: "he",
  to: "fr",
  saved: false
}];
const TALK = [{
  side: "he",
  text: "סליחה, איך מגיעים למוזיאון?",
  alt: "Excuse me, how do I get to the museum?"
}, {
  side: "en",
  text: "Take the number 5 bus, four stops.",
  alt: "קח את קו 5, ארבע תחנות."
}, {
  side: "he",
  text: "תודה רבה",
  alt: "Thank you very much"
}];
const ACTIONS = [{
  icon: "volume_up",
  label: "השמע"
}, {
  icon: "content_copy",
  label: "העתק"
}, {
  icon: "share",
  label: "שתף"
}, {
  icon: "star",
  label: "שמור"
}];

/* the chevron points left: left is forward in RTL */
const Chevron = () => /*#__PURE__*/React.createElement(Icon, {
  name: "keyboard_arrow_left",
  size: 36,
  color: "var(--fos-text-30)"
});
const Star = () => /*#__PURE__*/React.createElement(Icon, {
  name: "star",
  size: 32,
  color: "var(--fos-favorite)"
});
const capsule = focused => ({
  flex: 1,
  minWidth: 0,
  minHeight: 96,
  borderRadius: 999,
  background: focused ? "var(--fos-accent-20)" : "var(--fos-text-08)",
  border: "4px solid " + (focused ? "var(--fos-accent)" : "transparent"),
  boxSizing: "border-box",
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  gap: 2,
  padding: "var(--fos-space-2) var(--fos-space-4)",
  cursor: "pointer",
  transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)"
});
const capLabel = {
  fontSize: "var(--fos-size-summary)",
  color: "var(--fos-text-55)",
  letterSpacing: "var(--fos-tracking-section)"
};
const capName = {
  fontSize: "var(--fos-size-title)",
  fontWeight: "var(--fos-weight-medium)",
  whiteSpace: "nowrap",
  overflow: "hidden",
  textOverflow: "ellipsis",
  maxWidth: "100%"
};
const sectionLabel = {
  fontSize: "var(--fos-size-summary)",
  color: "var(--fos-text-55)",
  letterSpacing: "var(--fos-tracking-section)"
};
function LangBar({
  from,
  to,
  focus,
  sub,
  onPick,
  onSwap
}) {
  const f = i => focus === 0 && sub === i;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: "var(--fos-space-3)",
      padding: "0 var(--fos-space-screen) var(--fos-space-5)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: capsule(f(0)),
    onClick: () => onPick("from")
  }, /*#__PURE__*/React.createElement("div", {
    style: capLabel
  }, "\u05DE"), /*#__PURE__*/React.createElement("div", {
    style: capName
  }, from.name)), /*#__PURE__*/React.createElement(IconButton, {
    icon: "swap_horiz",
    focused: f(1),
    onClick: onSwap
  }), /*#__PURE__*/React.createElement("div", {
    style: capsule(f(2)),
    onClick: () => onPick("to")
  }, /*#__PURE__*/React.createElement("div", {
    style: capLabel
  }, "\u05D0\u05DC"), /*#__PURE__*/React.createElement("div", {
    style: capName
  }, to.name)));
}
function TranslateScreen({
  from,
  to,
  text,
  out,
  focus,
  sub,
  typing,
  speaking,
  saved,
  onPick,
  onSwap,
  onFocusInput,
  onAction,
  onOpenHistory
}) {
  const dir = l => l.rtl ? "rtl" : "ltr";
  const align = l => l.rtl ? "right" : "left";
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05EA\u05E8\u05D2\u05D5\u05DD",
    onMenu: () => {}
  }), /*#__PURE__*/React.createElement("div", {
    "data-f": "0"
  }, /*#__PURE__*/React.createElement(LangBar, {
    from: from,
    to: to,
    focus: focus,
    sub: sub,
    onPick: onPick,
    onSwap: onSwap
  })), /*#__PURE__*/React.createElement("div", {
    "data-f": "1"
  }, /*#__PURE__*/React.createElement(Card, null, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "var(--fos-space-5) var(--fos-space-6)",
      display: "flex",
      flexDirection: "column",
      gap: "var(--fos-space-3)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "baseline",
      justifyContent: "space-between"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: sectionLabel
  }, from.name), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-summary)",
      color: "var(--fos-text-40)",
      direction: "ltr"
    }
  }, text.length, "/1000")), /*#__PURE__*/React.createElement("div", {
    onClick: onFocusInput,
    style: {
      background: "var(--fos-idle-bg-field)",
      borderRadius: "var(--fos-radius-textfield)",
      border: "var(--fos-focus-border-control) solid " + (focus === 1 ? "var(--fos-accent)" : "transparent"),
      padding: "var(--fos-space-5)",
      minHeight: 150,
      boxSizing: "border-box",
      cursor: "pointer",
      fontSize: "var(--fos-size-base)",
      lineHeight: "var(--fos-line-height)",
      color: text ? "var(--fos-text)" : "var(--fos-text-40)",
      direction: dir(from),
      textAlign: align(from),
      textWrap: "pretty",
      transition: "border-color var(--fos-transition-focus)"
    }
  }, text || "הקלד טקסט לתרגום", typing && /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-block",
      width: 3,
      height: 34,
      background: "var(--fos-accent)",
      verticalAlign: "-6px",
      marginInlineStart: 6
    }
  }))))), text ? /*#__PURE__*/React.createElement("div", {
    "data-f": "2"
  }, /*#__PURE__*/React.createElement(Card, null, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "var(--fos-space-5) var(--fos-space-6)",
      display: "flex",
      flexDirection: "column",
      gap: "var(--fos-space-4)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: sectionLabel
  }, to.name), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-title)",
      fontWeight: "var(--fos-weight-medium)",
      lineHeight: "var(--fos-line-height)",
      direction: dir(to),
      textAlign: align(to),
      textWrap: "pretty"
    }
  }, out), speaking && /*#__PURE__*/React.createElement(ProgressBar, {
    value: 0.35
  })), /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      padding: "var(--fos-space-2) var(--fos-space-5)",
      gap: "var(--fos-space-3)"
    }
  }, ACTIONS.map((a, i) => {
    const f = focus === 2 && sub === i;
    const isSaved = a.icon === "star" && saved;
    return /*#__PURE__*/React.createElement("div", {
      key: a.icon,
      onClick: () => onAction(i),
      style: {
        flex: 1,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: 4,
        padding: "var(--fos-space-2) 0",
        borderRadius: "var(--fos-radius-item)",
        background: f ? "var(--fos-accent-20)" : "transparent",
        border: "4px solid " + (f ? "var(--fos-accent)" : "transparent"),
        boxSizing: "border-box",
        cursor: "pointer",
        transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)"
      }
    }, /*#__PURE__*/React.createElement(Icon, {
      name: a.icon,
      size: 40,
      color: isSaved ? "var(--fos-favorite)" : "var(--fos-accent)"
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        fontSize: "var(--fos-size-label)",
        color: "var(--fos-text-60)"
      }
    }, a.label));
  })))) : /*#__PURE__*/React.createElement(EmptyState, {
    icon: "translate",
    title: "\u05D0\u05D9\u05DF \u05DE\u05D4 \u05DC\u05EA\u05E8\u05D2\u05DD",
    subtitle: "\u05DC\u05D7\u05E5 \u05E2\u05DC \u05D0\u05D9\u05E9\u05D5\u05E8 \u05D1\u05E9\u05D3\u05D4 \u05DB\u05D3\u05D9 \u05DC\u05D4\u05E7\u05DC\u05D9\u05D3"
  }), /*#__PURE__*/React.createElement(SectionHeader, null, "\u05D0\u05D7\u05E8\u05D5\u05E0\u05D5\u05EA"), /*#__PURE__*/React.createElement(Card, null, HISTORY.slice(0, 2).map((h, i) => /*#__PURE__*/React.createElement(React.Fragment, {
    key: h.src
  }, i > 0 && /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement("div", {
    "data-f": 3 + i
  }, /*#__PURE__*/React.createElement(ListItem, {
    title: h.src,
    summary: h.dst,
    focused: focus === 3 + i,
    trailing: h.saved ? /*#__PURE__*/React.createElement(Star, null) : /*#__PURE__*/React.createElement(Chevron, null),
    onClick: () => onOpenHistory(h)
  }))))));
}
function LangPickerScreen({
  role,
  query,
  focus,
  current,
  typing,
  onBack,
  onSelect
}) {
  const q = query.trim();
  const list = LANGS.filter(l => l.name.includes(q) && (role === "from" || l.code !== "auto"));
  const recents = q ? [] : RECENT_LANGS.map(byCode);
  let idx = 0;
  const row = (l, key) => {
    const i = ++idx;
    return /*#__PURE__*/React.createElement("div", {
      "data-f": i,
      key: key
    }, /*#__PURE__*/React.createElement(ListItem, {
      title: l.name,
      focused: focus === i,
      trailing: l.code === current ? /*#__PURE__*/React.createElement(Icon, {
        name: "check",
        size: 36,
        color: "var(--fos-accent)"
      }) : null,
      onClick: () => onSelect(l.code)
    }));
  };
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: role === "from" ? "תרגם מ" : "תרגם אל",
    onBack: onBack
  }), /*#__PURE__*/React.createElement("div", {
    "data-f": "0",
    style: {
      padding: "0 var(--fos-space-screen) var(--fos-space-2)"
    }
  }, /*#__PURE__*/React.createElement(TextField, {
    value: query,
    placeholder: "\u05D7\u05E4\u05E9 \u05E9\u05E4\u05D4",
    focused: focus === 0,
    showCaret: typing
  })), recents.length > 0 && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(SectionHeader, null, "\u05D0\u05D7\u05E8\u05D5\u05E0\u05D5\u05EA"), /*#__PURE__*/React.createElement(Card, null, recents.map((l, i) => /*#__PURE__*/React.createElement(React.Fragment, {
    key: "r" + l.code
  }, i > 0 && /*#__PURE__*/React.createElement(Divider, null), row(l, "r" + l.code))))), /*#__PURE__*/React.createElement(SectionHeader, null, "\u05DB\u05DC \u05D4\u05E9\u05E4\u05D5\u05EA"), list.length === 0 ? /*#__PURE__*/React.createElement(EmptyState, {
    icon: "search_off",
    title: "\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D4 \u05E9\u05E4\u05D4",
    subtitle: "\u05E0\u05E1\u05D4 \u05DE\u05D9\u05DC\u05D4 \u05D0\u05D7\u05E8\u05EA"
  }) : /*#__PURE__*/React.createElement(Card, null, list.map((l, i) => /*#__PURE__*/React.createElement(React.Fragment, {
    key: l.code
  }, i > 0 && /*#__PURE__*/React.createElement(Divider, null), row(l, l.code)))));
}
function HistoryScreen({
  tab,
  focus,
  onBack,
  onTab,
  onOpen
}) {
  const list = tab === 1 ? HISTORY.filter(h => h.saved) : HISTORY;
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05D4\u05D9\u05E1\u05D8\u05D5\u05E8\u05D9\u05D4",
    onBack: onBack,
    onMenu: () => {}
  }), /*#__PURE__*/React.createElement("div", {
    "data-f": "0"
  }, /*#__PURE__*/React.createElement(TabRow, {
    items: ["הכול", "שמורים"],
    selected: tab,
    focusedIndex: focus === 0 ? tab : -1,
    onSelect: onTab
  })), list.length === 0 ? /*#__PURE__*/React.createElement(EmptyState, {
    icon: "bookmark",
    title: "\u05D0\u05D9\u05DF \u05EA\u05E8\u05D2\u05D5\u05DE\u05D9\u05DD \u05E9\u05DE\u05D5\u05E8\u05D9\u05DD",
    subtitle: "\u05DC\u05D7\u05E5 \u05E2\u05DC \u05E9\u05DE\u05D5\u05E8 \u05D1\u05EA\u05E8\u05D2\u05D5\u05DD \u05DB\u05D3\u05D9 \u05DC\u05D4\u05D5\u05E1\u05D9\u05E3"
  }) : /*#__PURE__*/React.createElement(Card, null, list.map((h, i) => /*#__PURE__*/React.createElement(React.Fragment, {
    key: h.src
  }, i > 0 && /*#__PURE__*/React.createElement(Divider, null), /*#__PURE__*/React.createElement("div", {
    "data-f": i + 1
  }, /*#__PURE__*/React.createElement(ListItem, {
    title: h.src,
    summary: h.dst + " · " + byCode(h.from).name + " ← " + byCode(h.to).name,
    focused: focus === i + 1,
    trailing: h.saved ? /*#__PURE__*/React.createElement(Star, null) : /*#__PURE__*/React.createElement(Chevron, null),
    onClick: () => onOpen(h)
  }))))));
}
function TalkScreen({
  listening,
  focus,
  onBack,
  onToggle
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      minHeight: 896,
      display: "flex",
      flexDirection: "column"
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: "\u05E9\u05D9\u05D7\u05D4",
    onBack: onBack,
    onMenu: () => {}
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: "flex",
      flexDirection: "column",
      gap: "var(--fos-space-5)",
      padding: "0 var(--fos-space-screen)"
    }
  }, TALK.map((t, i) => {
    const he = t.side === "he";
    return /*#__PURE__*/React.createElement("div", {
      key: i,
      style: {
        alignSelf: he ? "flex-start" : "flex-end",
        maxWidth: "84%",
        background: he ? "var(--fos-surface)" : "var(--fos-glass)",
        borderRadius: "var(--fos-radius-main)",
        padding: "var(--fos-space-5) var(--fos-space-6)",
        display: "flex",
        flexDirection: "column",
        gap: "var(--fos-space-2)"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: sectionLabel
    }, he ? "עברית" : "אנגלית"), /*#__PURE__*/React.createElement("div", {
      style: {
        fontSize: "var(--fos-size-base)",
        lineHeight: "var(--fos-line-height)",
        direction: he ? "rtl" : "ltr",
        textAlign: he ? "right" : "left"
      }
    }, t.text), /*#__PURE__*/React.createElement("div", {
      style: {
        fontSize: "var(--fos-size-body)",
        color: "var(--fos-text-60)",
        lineHeight: "var(--fos-line-height)",
        direction: he ? "ltr" : "rtl",
        textAlign: he ? "left" : "right"
      }
    }, t.alt));
  })), /*#__PURE__*/React.createElement("div", {
    "data-f": "0",
    style: {
      padding: "var(--fos-space-8) var(--fos-space-screen) var(--fos-space-9)",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: "var(--fos-space-4)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    onClick: onToggle,
    style: {
      width: 168,
      height: 168,
      borderRadius: 999,
      background: listening ? "var(--fos-accent-20)" : "var(--fos-text-08)",
      border: "4px solid " + (focus === 0 ? "var(--fos-accent)" : "transparent"),
      boxSizing: "border-box",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      cursor: "pointer"
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: listening ? "graphic_eq" : "mic",
    size: 80,
    color: listening ? "var(--fos-accent)" : "var(--fos-text-60)"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--fos-size-body)",
      color: "var(--fos-text-60)"
    }
  }, listening ? "מקשיב · עברית" : "לחץ על אישור כדי לדבר")));
}
Object.assign(window, {
  LANGS,
  HISTORY,
  TALK,
  ACTIONS,
  byCode,
  TranslateScreen,
  LangPickerScreen,
  HistoryScreen,
  TalkScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/translate/TranslateScreens.jsx", error: String((e && e.message) || e) }); }

__ds_ns.ActionGrid = __ds_scope.ActionGrid;

__ds_ns.Avatar = __ds_scope.Avatar;

__ds_ns.Button = __ds_scope.Button;

__ds_ns.Card = __ds_scope.Card;

__ds_ns.Divider = __ds_scope.Divider;

__ds_ns.EmptyState = __ds_scope.EmptyState;

__ds_ns.FosIcon = __ds_scope.FosIcon;

__ds_ns.FOS_ICON_NAMES = __ds_scope.FOS_ICON_NAMES;

__ds_ns.Icon = __ds_scope.Icon;

__ds_ns.IconButton = __ds_scope.IconButton;

__ds_ns.ListItem = __ds_scope.ListItem;

__ds_ns.MonoValue = __ds_scope.MonoValue;

__ds_ns.ScreenHero = __ds_scope.ScreenHero;

__ds_ns.SectionHeader = __ds_scope.SectionHeader;

__ds_ns.TopBar = __ds_scope.TopBar;

__ds_ns.Widget = __ds_scope.Widget;

__ds_ns.Badge = __ds_scope.Badge;

__ds_ns.ConfirmDialog = __ds_scope.ConfirmDialog;

__ds_ns.GlassPanel = __ds_scope.GlassPanel;
__ds_ns.GlassTile = __ds_scope.GlassTile;

__ds_ns.HeadsUpNotification = __ds_scope.HeadsUpNotification;

__ds_ns.InputDialog = __ds_scope.InputDialog;

__ds_ns.NotificationCard = __ds_scope.NotificationCard;

__ds_ns.ProgressBar = __ds_scope.ProgressBar;

__ds_ns.Snackbar = __ds_scope.Snackbar;

__ds_ns.Spinner = __ds_scope.Spinner;

__ds_ns.Capsule = __ds_scope.Capsule;

__ds_ns.Checkbox = __ds_scope.Checkbox;

__ds_ns.Chip = __ds_scope.Chip;

__ds_ns.DatePicker = __ds_scope.DatePicker;

__ds_ns.DayChip = __ds_scope.DayChip;

__ds_ns.RadioButton = __ds_scope.RadioButton;

__ds_ns.SettingItem = __ds_scope.SettingItem;

__ds_ns.Slider = __ds_scope.Slider;

__ds_ns.Switch = __ds_scope.Switch;

__ds_ns.TextArea = __ds_scope.TextArea;

__ds_ns.TextField = __ds_scope.TextField;

__ds_ns.TimePicker = __ds_scope.TimePicker;

__ds_ns.ToggleButton = __ds_scope.ToggleButton;

__ds_ns.BottomNav = __ds_scope.BottomNav;

__ds_ns.OptionsMenu = __ds_scope.OptionsMenu;

__ds_ns.SoftKeyBar = __ds_scope.SoftKeyBar;

__ds_ns.StatusCapsule = __ds_scope.StatusCapsule;

__ds_ns.TabRow = __ds_scope.TabRow;

})();
