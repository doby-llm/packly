package com.dobyllm.packly.core.validation

fun requireName(value: String, label: String = "Name"): String? = if (value.trim().isEmpty()) "$label is required." else null
fun requirePositiveQuantity(value: Int): String? = if (value < 1) "Quantity must be at least 1." else null
