package com.future.dialer.ui.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    /** יומן השיחות - הטאב הראשון, לפי ui_kits/calls ("log first"). */
    object CallLog : Screen("calllog")
    object Dialpad : Screen("dialpad")
    object Contacts : Screen("contacts")
    object InCall : Screen("incall/{name}/{number}") {
        fun createRoute(name: String, number: String): String {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            val encodedNumber = URLEncoder.encode(number, "UTF-8")
            return "incall/$encodedName/$encodedNumber"
        }
    }
}

