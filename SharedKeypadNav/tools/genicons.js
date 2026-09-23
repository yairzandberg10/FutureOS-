const fs = require('fs');
const src = fs.readFileSync(process.argv[2], 'utf8');
const re = /^  ([a-z_0-9]+): "([^"]+)",?$/gm;
let m; const icons = [];
while ((m = re.exec(src))) icons.push([m[1], m[2]]);
const pascal = s => s.split('_').map(p => p[0].toUpperCase() + p.slice(1)).join('');
// Material's AutoMirrored set among the drawn names: these flip in RTL exactly
// like the Icons.AutoMirrored.Rounded.* they replace.
const mirrored = new Set(['arrow_back','arrow_forward','keyboard_arrow_left','keyboard_arrow_right','call_made','call_received','call_missed','send','chat','trending_up','help','volume_up','volume_off']);
// Closed shapes the DS draws filled (fill={1}), matching the filled Material glyph they replace.
const solid = new Set(['star','bookmark']);
let out = `package com.future.sharednav.icons

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * "Future Glyphs" - סט האייקונים של ה-Design System
 * (design/FutureOS Design System/components/core/FosIcon.jsx), מומר ל-ImageVector.
 * רשת 24, קו 1.6, קצוות ומפרקים מעוגלים. השמות זהים לשמות Material Symbols
 * שהיו בשימוש (Icons.Rounded.Home -> FutureIcons.Home), כך ששם שלא צויר בסט
 * נשאר ב-Icons.Rounded ולא נשבר שום מסך.
 *
 * הקובץ נוצר אוטומטית מ-FosIcon.jsx - לשינוי אייקון יש לעדכן את ה-DS ולהריץ
 * מחדש את המחולל, לא לערוך כאן ידנית.
 */
object FutureIcons {
`;
for (const [name, spec] of icons) {
  const id = pascal(name);
  const parts = spec.split('|').map(p => JSON.stringify(p));
  out += `    val ${id}: ImageVector by lazy { futureGlyph("${id}", listOf(${parts.join(', ')})${solid.has(name) ? ', solid = true' : ''}) }\n`;
}
out += `\n    /** הגרסאות שמתהפכות ב-RTL, במקום Icons.AutoMirrored.Rounded.* */\n    object AutoMirrored {\n`;
for (const [name, spec] of icons) {
  if (!mirrored.has(name)) continue;
  const id = pascal(name);
  const parts = spec.split('|').map(p => JSON.stringify(p));
  out += `        val ${id}: ImageVector by lazy { futureGlyph("AutoMirrored.${id}", listOf(${parts.join(', ')}), autoMirror = true) }\n`;
}
out += `    }\n}\n`;
fs.writeFileSync(process.argv[3], out);
fs.writeFileSync(process.argv[4], icons.map(([n]) => pascal(n)).join('\n') + '\n');
console.log(icons.length + ' icons');
