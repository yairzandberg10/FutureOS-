package com.future.navigation.data.poi

import com.future.navigation.data.common.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** פירוק תשובת Overpass (קובץ לדוגמה בצורת הפלט של השרת) ובניית השאילתה. */
class OverpassParserTest {

    private val origin = LatLng(32.0800, 34.7800)

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResource("online/$name")!!.readText(Charsets.UTF_8)

    @Test
    fun parsesNodesAndCentersSortedByDistance() {
        val results = OverpassParser.parse(resource("overpass_fuel.json"), origin, PoiCategory.FUEL)

        // נקודה עם שם בעברית, שטח עם מותג (center), נקודה בלי שם (שם הקטגוריה).
        // הכפילות של "פז" באותו מקום נשמטת, ומקום מחוץ לישראל לא מופיע.
        assertEquals(listOf("פז", "Delek", "דלק"), results.map { it.name })
        assertTrue(results.zipWithNext().all { (a, b) -> a.distanceMeters <= b.distanceMeters })
        assertEquals(614.0, results[0].distanceMeters, 30.0)
        assertTrue(results.all { IsraelBounds.contains(it.location) })
    }

    @Test
    fun garbageOrEmptyResponseGivesNoResults() {
        assertTrue(OverpassParser.parse("not json", origin, PoiCategory.FUEL).isEmpty())
        assertTrue(OverpassParser.parse("{}", origin, PoiCategory.FUEL).isEmpty())
        assertTrue(OverpassParser.parse("""{"elements":[]}""", origin, PoiCategory.FUEL).isEmpty())
    }

    /** תשובה אמיתית של Overpass לאותה שאילתה שהאפליקציה בונה (קפה ומסעדה, 1 ק"מ מתל אביב). */
    @Test
    fun parsesRealCafeAnswerWithinRadiusAndSorted() {
        val results = OverpassParser.parse(resource("overpass_cafe.json"), origin, PoiCategory.CAFE_RESTAURANT)

        assertTrue(results.size > 10)
        assertTrue(results.size <= OverpassParser.MAX_RESULTS)
        assertTrue(results.zipWithNext().all { (a, b) -> a.distanceMeters <= b.distanceMeters })
        // השאילתה מוגבלת לרדיוס של 1 ק"מ; מרכז שטח (way) יכול לזוז מעט
        assertTrue(results.all { it.distanceMeters <= 1100.0 })
        assertTrue(results.all { IsraelBounds.contains(it.location) && it.name.isNotBlank() })
    }

    @Test
    fun queryUsesCategoryRadiusAndAllCategoryTags() {
        val query = OverpassParser.buildQuery(PoiCategory.CAFE_RESTAURANT, origin)
        assertTrue(query.contains("nwr[\"amenity\"=\"cafe\"](around:1000,32.08000,34.78000);"))
        assertTrue(query.contains("nwr[\"amenity\"=\"restaurant\"](around:1000,32.08000,34.78000);"))
        assertTrue(query.endsWith("out center 300;"))
        assertTrue(query.startsWith("[out:json][timeout:15];"))

        // בתי חולים מחפשים ברדיוס רחב יותר, כי הם נדירים
        assertTrue(OverpassParser.buildQuery(PoiCategory.HOSPITAL, origin).contains("(around:15000,"))
    }
}
