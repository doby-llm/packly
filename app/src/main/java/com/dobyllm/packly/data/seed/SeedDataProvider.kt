package com.dobyllm.packly.data.seed

import com.dobyllm.packly.core.model.*
import com.dobyllm.packly.ui.token.CategoryTokens

object SeedDataProvider {
    private const val seedTime = "2026-01-01T00:00:00Z"

    val categories: List<PacklyCategory> = CategoryTokens.all.mapIndexed { index, token ->
        PacklyCategory(
            id = "cat_${token.key}", key = token.key, label = token.label, iconKey = token.iconKey,
            accentColorHex = token.accentHex, softColorHex = token.softHex, sortOrder = index, isSeed = true,
        )
    }

    val items: List<PacklyItem> = listOf(
        item("tshirts", "T-shirts", "clothing"), item("underwear", "Underwear", "clothing"), item("socks", "Socks", "clothing"), item("pants", "Pants / shorts", "clothing"), item("hoodie", "Sweater or hoodie", "clothing"), item("sleepwear", "Sleepwear", "clothing"), item("shoes", "Comfortable shoes", "clothing"),
        item("toothbrush", "Toothbrush", "toiletries"), item("toothpaste", "Toothpaste", "toiletries"), item("shampoo", "Shampoo", "toiletries"), item("deodorant", "Deodorant", "toiletries"), item("hairbrush", "Hairbrush / comb", "toiletries"),
        item("phone_charger", "Phone charger", "electronics"), item("power_bank", "Power bank", "electronics"), item("headphones", "Headphones", "electronics"), item("laptop_charger", "Laptop charger", "electronics"), item("travel_adapter", "Travel adapter", "electronics"),
        item("passport", "Passport / ID", "documents"), item("tickets", "Tickets / boarding pass", "documents"), item("wallet", "Wallet", "documents"), item("insurance", "Travel insurance card", "documents"),
        item("medication", "Medication", "health"), item("sunscreen", "Sunscreen", "health"), item("first_aid", "First-aid basics", "health"),
        item("water_bottle", "Water bottle", "comfort"), item("book", "Book / e-reader", "comfort"), item("neck_pillow", "Neck pillow", "comfort"),
    )

    val lists: List<PacklyList> = listOf(
        list("weekend", "Weekend", "A light two-day starter list.", listOf("tshirts", "underwear", "socks", "pants", "toothbrush", "toothpaste", "deodorant", "phone_charger", "wallet")),
        list("beach_day", "Beach day", "Sunny-day essentials.", listOf("sunscreen", "water_bottle", "tshirts", "phone_charger", "wallet")),
        list("business", "Business trip", "Work travel basics.", listOf("passport", "tickets", "wallet", "laptop_charger", "phone_charger", "toothbrush", "deodorant")),
        list("camping", "Camping", "Simple outdoors checklist.", listOf("hoodie", "socks", "first_aid", "water_bottle", "power_bank")),
        list("family", "Family visit", "Friendly visit essentials.", listOf("tshirts", "underwear", "socks", "sleepwear", "toothbrush", "phone_charger", "book")),
    )

    fun initialDocument(): PacklyAppDocument = PacklyAppDocument(
        categories = categories,
        items = items,
        lists = lists,
        settings = PacklySettings(firstLaunchCompleted = true),
    )

    private fun item(key: String, name: String, categoryKey: String) = PacklyItem(
        id = "item_$key", name = name, categoryId = "cat_$categoryKey", isSeed = true,
        createdAt = seedTime, updatedAt = seedTime,
    )

    private fun list(key: String, name: String, description: String, itemKeys: List<String>): PacklyList = PacklyList(
        id = "list_$key", name = name, description = description, isSeed = true, createdAt = seedTime, updatedAt = seedTime,
        entries = itemKeys.mapIndexed { index, itemKey ->
            val item = items.first { it.id == "item_$itemKey" }
            PacklyListEntry("list_entry_${key}_$index", item.id, item.name, item.categoryId, sortOrder = index)
        },
    )
}
