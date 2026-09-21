package com.future.frixa.data

data class Store(
    val id: Int,
    val name: String,
    val address: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
)

object StoreCatalog {
    val all: List<Store> = listOf(
        Store(1, "שופרסל דיל", "רחוב הרצל 42", "סופרמרקט", 32.0809, 34.7806),
        Store(2, "רמי לוי", "דרך בן גוריון 15", "סופרמרקט", 32.0755, 34.7738),
        Store(3, "שוק הכרמל", "כרמל 12", "שוק ירקות", 32.0685, 34.7686),
        Store(4, "ויקטורי", "אלנבי 88", "סופרמרקט", 32.0668, 34.7699),
        Store(5, "חנות תבלינים המזרח", "לוינסקי 5", "תבלינים", 32.0596, 34.7699),
        Store(6, "יינות ביתן", "אבן גבירול 100", "סופרמרקט", 32.0838, 34.7822),
        Store(7, "קצביית הכפר", "דיזנגוף 190", "בשר ודגים", 32.0868, 34.7743),
        Store(8, "מעדניית הגליל", "בזל 22", "מעדנייה", 32.0900, 34.7810),
    )
}
