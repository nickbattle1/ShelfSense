package com.example.shelfsense.navigation

import android.net.Uri
import com.example.shelfsense.domain.PantryFilter

// string routes, as in the unit's navigation labs. arguments are optional query parameters
// so a destination can be reached with or without them
object Routes {
    const val AUTH_GRAPH = "auth"
    const val MAIN_GRAPH = "main"

    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val FORGOT_PASSWORD = "forgot?email={email}"
    // sits outside both graphs, between signing in and the app itself
    const val VERIFY_EMAIL = "verify"

    const val HOME = "home"
    const val PANTRY = "pantry?filter={filter}"
    const val INSIGHTS = "insights"
    const val PROFILE = "profile"

    const val ADD_FOOD = "add"
    const val SCAN = "add/scan"
    const val ENTER_BARCODE = "add/barcode"
    const val FOOD_FORM = "form?barcode={barcode}&itemId={itemId}"
    const val DETAIL = "item/{itemId}"

    const val ARG_EMAIL = "email"
    const val ARG_FILTER = "filter"
    const val ARG_BARCODE = "barcode"
    const val ARG_ITEM_ID = "itemId"

    // the same patterns the reminder notifications open
    const val DEEP_LINK_ITEM = "shelfsense://item/{itemId}"
    const val DEEP_LINK_PANTRY = "shelfsense://pantry?filter={filter}"

    fun forgotPassword(email: String) = "forgot?email=${Uri.encode(email)}"

    fun pantry(filter: PantryFilter = PantryFilter.ALL) = "pantry?filter=${filter.name}"

    fun detail(itemId: String) = "item/$itemId"

    fun foodForm(barcode: String? = null, itemId: String? = null): String = when {
        itemId != null -> "form?itemId=$itemId"
        barcode != null -> "form?barcode=$barcode"
        else -> "form"
    }
}
