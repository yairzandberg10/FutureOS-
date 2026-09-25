package com.future.contact.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import java.io.File

/** הכרטיס של המשתמש עצמו - לשונית "אני". */
data class MyProfile(
    val name: String = "",
    val description: String = "",
    val photoUri: String? = null,
)

/** מצב SIM אחד, כפי שמוצג בלשונית "אני". */
data class SimStatus(
    val slotLabel: String,
    val state: String,
    val carrier: String?,
    val network: String?,
    val number: String?,
    val signalBars: Int?,
    val roaming: Boolean,
)

/**
 * הפרופיל נשמר באפליקציה (לא ב-ContactsContract.Profile, שדורש הרשאות
 * פרופיל נפרדות). התמונה נשמרת כקובץ בשם חדש בכל החלפה - FutureAvatar
 * זוכר תמונות לפי הכתובת, ושם קבוע היה מציג את התמונה הישנה.
 */
class MyProfileStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("my_profile", Context.MODE_PRIVATE)

    fun load(): MyProfile = MyProfile(
        name = prefs.getString("name", "").orEmpty(),
        description = prefs.getString("description", "").orEmpty(),
        photoUri = prefs.getString("photo", null)?.takeIf { File(Uri.parse(it).path.orEmpty()).exists() },
    )

    fun setName(name: String) = prefs.edit().putString("name", name).apply()

    fun setDescription(description: String) = prefs.edit().putString("description", description).apply()

    fun setPhoto(image: Uri): Boolean = try {
        val source = ImageDecoder.createSource(context.contentResolver, image)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val scale = maxOf(1, minOf(info.size.width, info.size.height) / PHOTO_PX)
            decoder.setTargetSize(info.size.width / scale, info.size.height / scale)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val side = minOf(bitmap.width, bitmap.height)
        val square = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
        removePhotoFile()
        val file = File(context.filesDir, "me_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { square.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        prefs.edit().putString("photo", Uri.fromFile(file).toString()).apply()
        true
    } catch (e: Exception) {
        android.util.Log.e("MyProfileStore", "Error saving profile photo", e)
        false
    }

    fun removePhoto() {
        removePhotoFile()
        prefs.edit().remove("photo").apply()
    }

    private fun removePhotoFile() {
        prefs.getString("photo", null)?.let { Uri.parse(it).path }?.let { File(it).delete() }
    }

    fun hasPhoneStatePermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    /**
     * מצב כרטיסי ה-SIM. עם הרשאת READ_PHONE_STATE - כל SIM פעיל בנפרד (שם,
     * ספק, מספר); בלעדיה - מה ש-TelephonyManager נותן בלי הרשאה, ל-SIM הראשי.
     */
    @SuppressLint("MissingPermission", "HardwareIds")
    fun simStatuses(): List<SimStatus> {
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return emptyList()
        val canReadNumbers = context.checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED
        val subscriptions = if (hasPhoneStatePermission()) {
            runCatching { context.getSystemService(SubscriptionManager::class.java)?.activeSubscriptionInfoList }.getOrNull().orEmpty()
        } else emptyList()

        if (subscriptions.isEmpty()) {
            return listOf(statusOf(telephony, "SIM", number = null))
        }
        return subscriptions.map { info ->
            val perSim = telephony.createForSubscriptionId(info.subscriptionId)
            val number = if (canReadNumbers) runCatching { perSim.line1Number }.getOrNull()?.takeIf { it.isNotBlank() } else null
            statusOf(perSim, "SIM ${info.simSlotIndex + 1}", number ?: info.number?.takeIf { it.isNotBlank() })
        }
    }

    private fun statusOf(telephony: TelephonyManager, label: String, number: String?): SimStatus = SimStatus(
        slotLabel = label,
        state = when (telephony.simState) {
            TelephonyManager.SIM_STATE_READY -> "פעיל"
            TelephonyManager.SIM_STATE_ABSENT -> "אין כרטיס SIM"
            TelephonyManager.SIM_STATE_PIN_REQUIRED -> "נעול - נדרש PIN"
            TelephonyManager.SIM_STATE_PUK_REQUIRED -> "נעול - נדרש PUK"
            TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "נעול לרשת"
            TelephonyManager.SIM_STATE_NOT_READY -> "לא מוכן"
            TelephonyManager.SIM_STATE_PERM_DISABLED -> "מושבת"
            TelephonyManager.SIM_STATE_CARD_IO_ERROR -> "שגיאת כרטיס"
            else -> "לא ידוע"
        },
        carrier = telephony.simOperatorName?.takeIf { it.isNotBlank() },
        network = telephony.networkOperatorName?.takeIf { it.isNotBlank() },
        number = number,
        signalBars = runCatching { telephony.signalStrength?.level }.getOrNull(),
        roaming = runCatching { telephony.isNetworkRoaming }.getOrDefault(false),
    )

    private companion object {
        const val PHOTO_PX = 720
    }
}
