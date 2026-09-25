package com.future.dialer.data.model

data class Contact(
    val id: String,
    val name: String = "",
    val phoneNumber: String = "",
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
    /** כל המספרים של איש הקשר - לזיהוי שיחה מכל אחד מהם, לא רק מהראשון. */
    val allNumbers: List<String> = listOf(phoneNumber),
)
