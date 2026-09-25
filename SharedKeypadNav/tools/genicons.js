// Generates FutureIcons.kt from the DS icon set (FosIcon.jsx).
// usage: node genicons.js <FosIcon.jsx> <FutureIcons.kt> <names.txt>
const fs = require('fs');
const src = fs.readFileSync(process.argv[2], 'utf8');
// The shared shape constants (C8, SLASH, ...) and the P table, evaluated as plain JS.
const start = src.search(/^const [A-Z0-9_]+ = /m);
const end = src.indexOf('\n};', src.indexOf('const P = {')) + 3;
const P = new Function(src.slice(start, end) + '\nreturn P;')();
const icons = Object.entries(P);
const pascal = s => s.split('_').map(p => p[0].toUpperCase() + p.slice(1)).join('');
// Material's AutoMirrored set among the drawn names: these flip in RTL exactly
// like the Icons.AutoMirrored.Rounded.* they replace.
const mirrored = new Set(['arrow_back','arrow_forward','keyboard_arrow_left','keyboard_arrow_right','call_made','call_received','call_missed','send','chat','trending_up','help','volume_up','volume_off',
  'volume_down','directions_run','directions_walk','queue_music','playlist_add','menu_book','library_books','view_list','sort']);
// Closed shapes the DS draws filled (fill={1}), matching the filled Material glyph they replace.
const solid = new Set(['star','bookmark','favorite','fiber_manual_record']);
// Material's outline twins of the filled ones: the same glyph with fill={0}.
const outline = { star: 'StarBorder', bookmark: 'BookmarkBorder', favorite: 'FavoriteBorder' };
const glyph = (id, spec, extra) =>
  `futureGlyph("${id}", listOf(${spec.split('|').map(p => JSON.stringify(p)).join(', ')})${extra})`;
let out = `package com.future.sharednav.icons

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * "Future Glyphs" - סט האייקונים של ה-Design System
 * (design/FutureOS Design System/components/core/FosIcon.jsx), מומר ל-ImageVector.
 * רשת 24, קו 1.6, קצוות ומפרקים מעוגלים. השמות זהים לשמות Material Symbols
 * שהיו בשימוש (Icons.Rounded.Home -> FutureIcons.Home), כך ששם שלא צויר בסט
 * נשאר ב-Icons.Rounded ולא נשבר שום מסך.
 *
 * הקובץ נוצר אוטומטית מ-FosIcon.jsx (SharedKeypadNav/tools/genicons.js) - לשינוי
 * אייקון יש לעדכן את ה-DS ולהריץ מחדש את המחולל, לא לערוך כאן ידנית.
 */
object FutureIcons {
`;
const names = [];
for (const [name, spec] of icons) {
  const id = pascal(name);
  names.push(id);
  out += `    val ${id}: ImageVector by lazy { ${glyph(id, spec, solid.has(name) ? ', solid = true' : '')} }\n`;
  if (outline[name]) {
    names.push(outline[name]);
    out += `    val ${outline[name]}: ImageVector by lazy { ${glyph(outline[name], spec, '')} }\n`;
  }
}
out += `\n    /** הגרסאות שמתהפכות ב-RTL, במקום Icons.AutoMirrored.Rounded.* */\n    object AutoMirrored {\n`;
for (const [name, spec] of icons) {
  if (!mirrored.has(name)) continue;
  const id = pascal(name);
  names.push('AutoMirrored.' + id);
  out += `        val ${id}: ImageVector by lazy { ${glyph('AutoMirrored.' + id, spec, ', autoMirror = true')} }\n`;
}
out += `    }\n}\n`;
fs.writeFileSync(process.argv[3], out);
fs.writeFileSync(process.argv[4], names.join('\n') + '\n');
console.log(icons.length + ' glyphs, ' + names.length + ' vectors');
