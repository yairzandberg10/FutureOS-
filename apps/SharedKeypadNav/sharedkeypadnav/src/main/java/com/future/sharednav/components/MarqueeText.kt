package com.future.sharednav.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * טקסט ארוך שעלול לגלוש במסך הקבוע של 640px (למשל שם קובץ ארוך, כותרת שיר,
 * נושא הודעה) - כשהוא ממוקד גולל אופקית (basicMarquee), אחרת נחתך עם
 * שלוש נקודות. עד כה לא היה רכיב משותף לזה - חלק מהמסכים פשוט חתכו טקסט
 * בלי שום דרך לקרוא את הסוף שלו.
 */
@Composable
fun MarqueeText(
    text: String,
    color: Color,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    Text(
        text = text,
        color = color,
        style = style,
        maxLines = 1,
        overflow = if (isFocused) TextOverflow.Clip else TextOverflow.Ellipsis,
        modifier = if (isFocused) modifier.basicMarquee() else modifier,
    )
}
