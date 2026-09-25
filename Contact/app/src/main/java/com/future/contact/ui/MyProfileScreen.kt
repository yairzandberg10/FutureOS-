package com.future.contact.ui

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.contact.data.MyProfile
import com.future.contact.data.MyProfileStore
import com.future.contact.data.SimStatus
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.InputDialog
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.share.FutureShare
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * לשונית "אני" (במקום "חסומים" - אנשי קשר חסומים מופיעים עכשיו ברשימה
 * הרגילה עם סימן חסימה): הכרטיס של המשתמש - תמונה, שם ותיאור שאפשר לערוך
 * ולשתף - ומצב כרטיסי ה-SIM.
 */
@Composable
fun MyProfileScreen(theme: FutureTheme) {
    val context = LocalContext.current
    val store = remember { MyProfileStore(context) }
    val scope = rememberCoroutineScope()
    val type = rememberFutureType()
    var profile by remember { mutableStateOf(MyProfile()) }
    var sims by remember { mutableStateOf(emptyList<SimStatus>()) }
    var hasPhoneState by remember { mutableStateOf(store.hasPhoneStatePermission()) }
    var editingName by remember { mutableStateOf(false) }
    var editingDescription by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            profile = withContext(Dispatchers.IO) { store.load() }
            sims = withContext(Dispatchers.IO) { store.simStatuses() }
            hasPhoneState = store.hasPhoneStatePermission()
        }
    }
    LaunchedEffect(Unit) { reload() }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) reload()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { reload() }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) { store.setPhoto(uri) }
            if (!ok) android.widget.Toast.makeText(context, "לא ניתן לשמור את התמונה", android.widget.Toast.LENGTH_SHORT).show()
            reload()
        }
    }
    fun pickPhoto() {
        val gallery = Intent(Intent.ACTION_PICK).setType("image/*").setPackage("com.future.gallery")
        val any = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
        val intent = if (gallery.resolveActivity(context.packageManager) != null) gallery else any
        runCatching { photoPicker.launch(intent) }
    }
    val myNumber = sims.firstNotNullOfOrNull { it.number }
    fun shareMe() {
        val text = listOfNotNull(profile.name.ifBlank { null }, myNumber, profile.description.ifBlank { null }).joinToString("\n")
        if (text.isBlank()) return
        FutureShare.open(context, Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "שיתוף הפרטים שלי")
    }

    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("אני", fontSize = type.screenTitle, fontWeight = FutureTypography.weightBold, color = theme.textColor)
            }
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = FutureDimens.spacingLg)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = FutureDimens.spacingSm, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
                ) {
                    FutureAvatar(
                        theme = theme,
                        name = profile.name.ifBlank { null },
                        icon = if (profile.name.isBlank()) FutureIcons.Person else null,
                        size = 88.dp,
                        photoUri = profile.photoUri,
                    )
                    Text(
                        profile.name.ifBlank { "השם שלי" },
                        color = if (profile.name.isBlank()) theme.mutedTextColor else theme.textColor,
                        fontSize = type.screenTitle,
                        fontWeight = FutureTypography.weightBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
                    )
                    if (profile.description.isNotBlank()) {
                        Text(
                            profile.description,
                            color = theme.mutedTextColor,
                            fontSize = type.body,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
                        )
                    }
                    if (myNumber != null) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(myNumber, color = theme.mutedTextColor, fontSize = type.body)
                        }
                    }
                }

                FutureCard(theme = theme) {
                    FutureSettingItem(
                        title = if (profile.name.isBlank()) "הוסף שם" else "ערוך שם",
                        icon = FutureIcons.Edit,
                        theme = theme,
                        focusRequester = first,
                        onClick = { editingName = true },
                    )
                    FutureDivider(theme = theme)
                    FutureSettingItem(
                        title = if (profile.description.isBlank()) "הוסף תיאור" else "ערוך תיאור",
                        summary = profile.description.ifBlank { null },
                        icon = FutureIcons.Description,
                        theme = theme,
                        onClick = { editingDescription = true },
                    )
                    FutureDivider(theme = theme)
                    FutureSettingItem(
                        title = if (profile.photoUri == null) "הגדר תמונת פרופיל" else "החלף תמונת פרופיל",
                        icon = FutureIcons.AddAPhoto,
                        theme = theme,
                        onClick = ::pickPhoto,
                    )
                    if (profile.photoUri != null) {
                        FutureDivider(theme = theme)
                        FutureSettingItem(
                            title = "הסר תמונה",
                            icon = FutureIcons.NoPhotography,
                            theme = theme,
                            showChevron = false,
                            onClick = { store.removePhoto(); reload() },
                        )
                    }
                    FutureDivider(theme = theme)
                    FutureSettingItem(title = "שתף את הפרטים שלי", icon = FutureIcons.Share, theme = theme, showChevron = false, onClick = ::shareMe)
                }

                FutureSectionHeader("כרטיס SIM", theme)
                FutureCard(theme = theme) {
                    // השורות לחיצות (מרעננות) - שורה בלי פוקוס לא נגללת לתצוגה במכשיר מקשים.
                    sims.forEachIndexed { index, sim ->
                        if (index > 0) FutureDivider(theme = theme)
                        FutureSettingItem(
                            title = "${sim.slotLabel} · ${sim.state}",
                            summary = listOfNotNull(
                                sim.carrier,
                                sim.network?.takeIf { it != sim.carrier }?.let { "רשת: $it" },
                                sim.signalBars?.let { "קליטה ${it}/4" },
                                if (sim.roaming) "נדידה" else null,
                                sim.number,
                            ).joinToString(" · ").ifBlank { null },
                            icon = FutureIcons.SimCard,
                            theme = theme,
                            showChevron = false,
                            onClick = ::reload,
                        )
                    }
                    if (!hasPhoneState) {
                        if (sims.isNotEmpty()) FutureDivider(theme = theme)
                        FutureSettingItem(
                            title = "הצג מספר ופרטי כל כרטיס",
                            summary = "דורש הרשאת טלפון",
                            icon = FutureIcons.Info,
                            theme = theme,
                            onClick = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS))
                            },
                        )
                    }
                }
            }
        }
    }

    if (editingName) {
        InputDialog(
            title = "השם שלי",
            theme = theme,
            initialValue = profile.name,
            allowBlank = true,
            onDismiss = { editingName = false },
            onConfirm = { store.setName(it); editingName = false; reload() },
        )
    }
    if (editingDescription) {
        InputDialog(
            title = "תיאור",
            theme = theme,
            initialValue = profile.description,
            placeholder = "למשל: זמין אחרי 18:00",
            allowBlank = true,
            onDismiss = { editingDescription = false },
            onConfirm = { store.setDescription(it); editingDescription = false; reload() },
        )
    }
}
