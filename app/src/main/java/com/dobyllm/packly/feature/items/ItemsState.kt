package com.dobyllm.packly.feature.items

import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.core.model.PacklyItem

data class ItemSection(val category: PacklyCategory, val items: List<PacklyItem>)
