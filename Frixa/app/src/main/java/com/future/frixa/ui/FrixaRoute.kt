package com.future.frixa.ui

sealed class FrixaRoute {
    data object Home : FrixaRoute()
    data object Recipes : FrixaRoute()
    data class RecipeDetail(val id: Int) : FrixaRoute()
    data object Stores : FrixaRoute()
}
