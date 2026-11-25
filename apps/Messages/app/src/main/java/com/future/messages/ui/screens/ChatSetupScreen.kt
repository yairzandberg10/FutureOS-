package com.future.messages.ui.screens

import com.future.sharednav.i18n.tr
import com.future.sharednav.systemui.StatusBarInset
import androidx.compose.ui.unit.dp

import android.app.Activity
import android.telephony.TelephonyManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import com.future.messages.chat.FutureChat
import com.future.messages.data.NumberActions
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureSpinner
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor

/**
 * הפעלת צ'אט FutureOS: אימות מספר הטלפון ב-SMS אחד שהמכשיר שולח לעצמו וקולט
 * לבד - בלי קוד להקליד ובלי דפדפן. מפתחות ההצפנה נוצרים במכשיר.
 */
@Composable
fun ChatSetupScreen(theme: FutureTheme, onBack: () -> Unit) {
    val context = LocalContext.current
    val state by FutureChat.state.collectAsState()

    var phone by remember { mutableStateOf(simNumber(context)) }
    var codeSent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmSignOut by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }
    val buttonFocus = remember { FocusRequester() }

    val listener = remember {
        object : FutureChat.VerificationListener {
            override fun onCodeSent() { busy = false; codeSent = true; error = null }
            override fun onVerified() { busy = false; error = null }
            override fun onError(message: String) { busy = false; codeSent = false; error = message }
        }
    }

    LaunchedEffect(state, codeSent) {
        runCatching { if (state == FutureChat.State.ACTIVE || codeSent) buttonFocus.requestFocus() else fieldFocus.requestFocus() }
    }

    CompositionLocalProvider(LocalLayoutDirection provides com.future.sharednav.i18n.Tr.layoutDirection) {
        Column(Modifier.fillMaxSize().background(theme.backgroundColor).padding(top = StatusBarInset.TITLE_GAP_DP.dp)) {
            ScreenTopBar(title = tr("צ'אט FutureOS"), textColor = theme.textColor, accentColor = theme.accentColor, onBack = onBack)
            Column(
                Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
            ) {
                when (state) {
                    FutureChat.State.NOT_CONFIGURED -> Body(
                        "הצ'אט עוד לא זמין במכשיר הזה.",
                        theme,
                    )
                    FutureChat.State.ACTIVE -> {
                        Body("הצ'אט פעיל עבור ${NumberActions.isolateLtr(FutureChat.myPhone() ?: "")}.", theme)
                        Body(
                            tr("הודעות לאנשי קשר עם FutureOS נשלחות באינטרנט, מוצפנות מקצה לקצה, עם \"נמסר\", \"נקרא\" ו\"מקליד...\". לכל נמען אחר, או כשאין רשת, ההודעה יוצאת כ-SMS."),
                            theme,
                        )
                        FutureButton(
                            tr("התנתק"), theme, { confirmSignOut = true },
                            variant = FutureButtonVariant.Destructive, fillMaxWidth = true, focusRequester = buttonFocus,
                        )
                    }
                    FutureChat.State.SIGNED_OUT -> {
                        if (!codeSent) {
                            Body(tr("הקלד את המספר שלך. יישלח אליו SMS אחד לאימות, והאפליקציה תקלוט אותו לבד."), theme)
                            FutureTextField(
                                phone, { phone = it }, theme,
                                placeholder = tr("מספר טלפון"),
                                focusRequester = fieldFocus,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            )
                            FutureButton(tr("אמת מספר"), theme, {
                                busy = true; error = null
                                FutureChat.startVerification(context as Activity, phone, listener)
                            }, fillMaxWidth = true, enabled = !busy && phone.isNotBlank())
                        } else {
                            Body(tr("נשלח SMS למספר שלך. מחכה שיגיע…"), theme)
                            FutureButton(tr("שנה מספר"), theme, { codeSent = false; busy = false },
                                variant = FutureButtonVariant.Secondary, fillMaxWidth = true, focusRequester = buttonFocus)
                        }
                        if (busy) FutureSpinner(theme, label = tr("מאמת…"))
                    }
                }
                error?.let { Text(it, color = theme.dangerColor, fontSize = FutureTypography.caption) }
            }
        }
    }

    // התנתקות מוחקת מפתחות והרשמה - לא בלחיצה אחת (הפוקוס ההתחלתי על "ביטול")
    if (confirmSignOut) {
        ConfirmDialog(
            message = "להתנתק מצ'אט FutureOS?",
            theme = theme,
            confirmLabel = tr("התנתק"),
            onCancel = { confirmSignOut = false },
            onConfirm = {
                confirmSignOut = false
                FutureChat.signOut(context)
            },
        )
    }
}

@Composable
private fun Body(text: String, theme: FutureTheme) {
    Text(text, color = theme.mutedTextColor, fontSize = FutureTypography.body)
}

@Suppress("MissingPermission", "HardwareIds")
private fun simNumber(context: android.content.Context): String =
    runCatching { context.getSystemService(TelephonyManager::class.java)?.line1Number }.getOrNull().orEmpty()
