package com.future.bluetooth.data

/** סוג המכשיר, לפי ה-BluetoothClass שלו - קובע את האייקון בשורה ובמסך המכשיר. */
enum class DeviceKind { Headphones, Speaker, Car, Phone, Computer, Keyboard, Mouse, Watch, Other }

/** מצב החיבור של מכשיר מותאם. [Connecting] כולל גם התאמה שעדיין בתהליך. */
enum class Connection { Disconnected, Connecting, Connected }

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isBonded: Boolean,
    val kind: DeviceKind = DeviceKind.Other,
    val connection: Connection = Connection.Disconnected,
    /** 0-100, או null כשהמכשיר לא מדווח סוללה. */
    val battery: Int? = null,
    /** המכשיר תומך בשיחות (HFP/HSP) - מציג את המתג "שיחות ואודיו". */
    val supportsCalls: Boolean = false,
    /** המכשיר תומך בהשמעת מדיה (A2DP) - מציג את המתג "מדיה". */
    val supportsMedia: Boolean = false,
    val callsConnected: Boolean = false,
    val mediaConnected: Boolean = false,
)
