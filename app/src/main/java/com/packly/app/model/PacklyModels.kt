package com.packly.app.model

import java.util.UUID

/**
 * Category for packing items — each maps to a Material icon and a brand color.
 *
 * Icon names reference material-icons-extended:
 *   implementation("androidx.compose.material:material-icons-extended")
 */
enum class Category(
    val displayName: String,
    val materialIconName: String,
    val description: String,
) {
    CLOTHING_TOP(
        displayName = "Tops",
        materialIconName = "Checkroom",
        description = "T-shirts, shirts, blouses, tank tops",
    ),
    CLOTHING_BOTTOM(
        displayName = "Bottoms",
        materialIconName = "Hiking",
        description = "Pants, shorts, skirts, jeans",
    ),
    SHOES_SOCKS(
        displayName = "Shoes & Socks",
        materialIconName = "Shoes",
        description = "Sneakers, sandals, flip-flops, socks",
    ),
    EXTRA_LAYERS(
        displayName = "Layers",
        materialIconName = "Checkroom",
        description = "Sweater, hoodie, jacket, cardigan",
    ),
    ACCESSORIES(
        displayName = "Accessories",
        materialIconName = "Sunglasses",
        description = "Sunglasses, cap, scarf, belt, jewelry",
    ),
    TECHNOLOGY(
        displayName = "Tech",
        materialIconName = "Charger",
        description = "Charger, laptop, power bank, headphones",
    ),
    TOILETRIES(
        displayName = "Toiletries",
        materialIconName = "Soap",
        description = "Toothbrush, shampoo, sunscreen, deodorant",
    ),
    DOCUMENTS(
        displayName = "Documents",
        materialIconName = "Article",
        description = "Passport, ID, tickets, hotel booking",
    ),
}

/**
 * A single packing item on a trip.
 */
data class PackingItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: Category,
    val quantity: Int = 1,
    val isPacked: Boolean = false,
    val notes: String = "",
)

/**
 * A trip — the top-level grouping for packing lists.
 */
data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val destination: String,
    val startDate: Long,      // epoch millis
    val endDate: Long,        // epoch millis
    val items: List&lt;PackingItem&gt; = emptyList(),
)
