package com.packly.app.data.seed

import com.packly.app.data.model.Category
import com.packly.app.data.model.Item

/**
 * Default seed data for first launch.
 * 8 categories + 25 default backlog items matching the architecture plan.
 */
object DefaultData {

    val categories: List<Category> = listOf(
        Category(id = "clothes", name = "Clothes", iconName = "checkroom", sortOrder = 0),
        Category(id = "shoes", name = "Shoes", iconName = "hiking", sortOrder = 1),
        Category(id = "accessories", name = "Accessories", iconName = "watch", sortOrder = 2),
        Category(id = "technology", name = "Technology", iconName = "devices", sortOrder = 3),
        Category(id = "toiletries", name = "Toiletries", iconName = "sanitizer", sortOrder = 4),
        Category(id = "documents", name = "Documents", iconName = "description", sortOrder = 5),
        Category(id = "extra_layers", name = "Extra Layers", iconName = "umbrella", sortOrder = 6),
        Category(id = "miscellaneous", name = "Miscellaneous", iconName = "more_horiz", sortOrder = 7),
    )

    val items: List<Item> = listOf(
        // -- Clothes --
        Item(id = "t-shirt", name = "T-Shirt", categoryId = "clothes", iconName = "checkroom", createdAt = 1704035200000),
        Item(id = "jeans", name = "Jeans", categoryId = "clothes", iconName = "checkroom", createdAt = 1704035200001),
        Item(id = "socks", name = "Socks", categoryId = "clothes", iconName = "socks", createdAt = 1704035200002),
        Item(id = "underwear", name = "Underwear", categoryId = "clothes", iconName = "accessibility", createdAt = 1704035200003),

        // -- Extra Layers --
        Item(id = "sweater", name = "Sweater", categoryId = "extra_layers", iconName = "umbrella", createdAt = 1704035200004),
        Item(id = "jacket", name = "Jacket", categoryId = "extra_layers", iconName = "umbrella", createdAt = 1704035200005),

        // -- Shoes --
        Item(id = "sneakers", name = "Sneakers", categoryId = "shoes", iconName = "hiking", createdAt = 1704035200006),
        Item(id = "sandals", name = "Sandals", categoryId = "shoes", iconName = "beach_access", createdAt = 1704035200007),

        // -- Accessories --
        Item(id = "watch", name = "Watch", categoryId = "accessories", iconName = "watch", createdAt = 1704035200008),
        Item(id = "sunglasses", name = "Sunglasses", categoryId = "accessories", iconName = "dark_mode", createdAt = 1704035200009),
        Item(id = "belt", name = "Belt", categoryId = "accessories", iconName = "more_horiz", createdAt = 1704035200010),

        // -- Technology --
        Item(id = "phone", name = "Phone", categoryId = "technology", iconName = "smartphone", createdAt = 1704035200011),
        Item(id = "charger", name = "Charger", categoryId = "technology", iconName = "cable", createdAt = 1704035200012),
        Item(id = "power_bank", name = "Power Bank", categoryId = "technology", iconName = "battery_charging_full", createdAt = 1704035200013),
        Item(id = "headphones", name = "Headphones", categoryId = "technology", iconName = "headphones", createdAt = 1704035200014),
        Item(id = "laptop", name = "Laptop", categoryId = "technology", iconName = "laptop", createdAt = 1704035200015),

        // -- Toiletries --
        Item(id = "toothbrush", name = "Toothbrush", categoryId = "toiletries", iconName = "more_horiz", createdAt = 1704035200016),
        Item(id = "toothpaste", name = "Toothpaste", categoryId = "toiletries", iconName = "more_horiz", createdAt = 1704035200017),
        Item(id = "shampoo", name = "Shampoo", categoryId = "toiletries", iconName = "water_drop", createdAt = 1704035200018),
        Item(id = "deodorant", name = "Deodorant", categoryId = "toiletries", iconName = "air", createdAt = 1704035200019),
        Item(id = "sunscreen", name = "Sunscreen", categoryId = "toiletries", iconName = "wb_sunny", createdAt = 1704035200020),

        // -- Documents --
        Item(id = "passport", name = "Passport", categoryId = "documents", iconName = "description", createdAt = 1704035200021),
        Item(id = "boarding_pass", name = "Boarding Pass", categoryId = "documents", iconName = "flight", createdAt = 1704035200022),
        Item(id = "insurance_card", name = "Insurance Card", categoryId = "documents", iconName = "health_and_safety", createdAt = 1704035200023),

        // -- Extra Layers --
        Item(id = "scarf", name = "Scarf", categoryId = "extra_layers", iconName = "more_horiz", createdAt = 1704035200024),
        Item(id = "gloves", name = "Gloves", categoryId = "extra_layers", iconName = "more_horiz", createdAt = 1704035200025),
    )
}
