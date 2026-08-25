package com.future.navigation.data.geocoding

import kotlinx.serialization.Serializable

/**
 * צורת התשובה האמיתית של Nominatim (nominatim.openstreetmap.org/search),
 * נבדקה ידנית מול השרת האמיתי עם שאילתה בעברית ("עזריאל, תל אביב") לפני
 * שנכתב הקובץ - תוצאות אמיתיות עם קואורדינטות אמיתיות הוחזרו.
 */
@Serializable
data class NominatimResult(
    val display_name: String,
    val lat: String,
    val lon: String,
    val type: String = ""
)
