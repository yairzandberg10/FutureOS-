package com.future.fitness.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutStoreCalculationsTest {

    @Test
    fun `estimateCalories multiplies MET by weight and hours`() {
        // met=8, weight=80kg, 30 minutes (0.5h) => 8 * 80 * 0.5 = 320
        assertEquals(320, WorkoutStore.estimateCalories(met = 8.0, weightKg = 80, minutes = 30))
    }

    @Test
    fun `estimateCalories rounds to nearest whole calorie`() {
        // 5.0 * 70 * (10/60.0) = 58.333... -> rounds to 58
        assertEquals(58, WorkoutStore.estimateCalories(met = 5.0, weightKg = 70, minutes = 10))
    }

    @Test
    fun `estimateCalories returns zero for zero minutes`() {
        assertEquals(0, WorkoutStore.estimateCalories(met = 9.0, weightKg = 80, minutes = 0))
    }

    @Test
    fun `estimateMaxHr follows the 220-minus-age formula`() {
        assertEquals(185, WorkoutStore.estimateMaxHr(age = 35))
        assertEquals(220, WorkoutStore.estimateMaxHr(age = 0))
    }
}
