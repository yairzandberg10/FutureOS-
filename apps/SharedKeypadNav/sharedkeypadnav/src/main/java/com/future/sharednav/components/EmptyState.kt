package com.future.sharednav.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.future.sharednav.focus.staggeredEntrance
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.rememberFutureType

/**
 * מצב ריק אחיד (אייקון + כותרת + הסבר). האייקון, הכותרת וההסבר נכנסים
 * בדירוג קצר - בלי זה, מצב ריק שמופיע אחרי טעינה (רשימה שחזרה ריקה)
 * החליף את מסך הטעינה בבת אחת ונראה כמו הבהוב.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val type = rememberFutureType()
    Column(
        modifier = modifier.fillMaxSize().padding(FutureDimens.spacingXxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.4f),
            modifier = Modifier.staggeredEntrance(0).size(FutureDimens.iconEmptyState),
        )
        Text(
            title,
            color = textColor.copy(alpha = 0.7f),
            fontSize = type.title,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.staggeredEntrance(1).padding(top = FutureDimens.spacingLg),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                color = textColor.copy(alpha = 0.4f),
                fontSize = type.body,
                textAlign = TextAlign.Center,
                modifier = Modifier.staggeredEntrance(2).padding(top = FutureDimens.spacingXs),
            )
        }
    }
}
