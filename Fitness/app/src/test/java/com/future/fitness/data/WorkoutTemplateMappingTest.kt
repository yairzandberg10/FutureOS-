package com.future.fitness.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * נועל את מיפוי סוג-הפעילות->תבנית-מסך (WorkoutTemplate) - הלוגיקה שקובעת אילו
 * מסכי-משנה יוצגו בזמן אימון חי (ראו WorkoutTemplateScreen). רגרסיה כאן
 * (למשל ריצה שמקבלת בטעות את התבנית הכללית) לא הייתה נתפסת בקומפילציה.
 */
class WorkoutTemplateMappingTest {

    @Test
    fun `running and walking activities map to RUN_WALK template`() {
        assertEquals(WorkoutTemplate.RUN_WALK, WorkoutActivityTypes.byId("outdoor_run")!!.template())
        assertEquals(WorkoutTemplate.RUN_WALK, WorkoutActivityTypes.byId("indoor_run")!!.template())
        assertEquals(WorkoutTemplate.RUN_WALK, WorkoutActivityTypes.byId("outdoor_walk")!!.template())
        assertEquals(WorkoutTemplate.RUN_WALK, WorkoutActivityTypes.byId("indoor_walk")!!.template())
    }

    @Test
    fun `hiking and track and field also map to RUN_WALK template`() {
        assertEquals(WorkoutTemplate.RUN_WALK, WorkoutActivityTypes.byId("hiking")!!.template())
        assertEquals(WorkoutTemplate.RUN_WALK, WorkoutActivityTypes.byId("track_and_field")!!.template())
    }

    @Test
    fun `cycling activities map to CYCLING template`() {
        assertEquals(WorkoutTemplate.CYCLING, WorkoutActivityTypes.byId("outdoor_cycle")!!.template())
        assertEquals(WorkoutTemplate.CYCLING, WorkoutActivityTypes.byId("indoor_cycle")!!.template())
        assertEquals(WorkoutTemplate.CYCLING, WorkoutActivityTypes.byId("hand_cycling")!!.template())
    }

    @Test
    fun `everything else maps to the GENERAL template`() {
        assertEquals(WorkoutTemplate.GENERAL, WorkoutActivityTypes.byId("yoga")!!.template())
        assertEquals(WorkoutTemplate.GENERAL, WorkoutActivityTypes.byId("basketball")!!.template())
        assertEquals(WorkoutTemplate.GENERAL, WorkoutActivityTypes.byId("hiit")!!.template())
    }

    @Test
    fun `swimming was removed from the catalog entirely`() {
        assertFalse(WorkoutActivityTypes.all.any { it.id.contains("swim") })
        assertFalse(ActivityCategory.entries.any { it.name == "SWIMMING" })
    }

    @Test
    fun `gps-tracked activities using RUN_WALK template stay consistent with usesGps`() {
        val runWalkTypes = WorkoutActivityTypes.all.filter { it.template() == WorkoutTemplate.RUN_WALK }
        val gpsIds = setOf("outdoor_run", "outdoor_walk", "hiking", "track_and_field")
        for (type in runWalkTypes) {
            assertEquals("usesGps mismatch for ${type.id}", type.id in gpsIds, type.usesGps)
        }
    }
}
