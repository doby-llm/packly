package com.dobyllm.packly.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.dobyllm.packly.core.i18n.supportedPacklyLocale
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.core.model.PacklyList
import com.dobyllm.packly.core.model.PacklyListEntry
import com.dobyllm.packly.core.model.TripEntry
import java.util.Locale

@Composable
fun PacklyCategory.displayLabel(): String = seedCategoryLabel(key, label).localized()

@Composable
fun PacklyItem.displayName(): String = localizedSeedText(id, name, SeedItemNames) ?: name

@Composable
fun PacklyList.displayName(): String = localizedSeedText(id, name, SeedListNames) ?: name

@Composable
fun PacklyList.displayDescription(): String = localizedSeedText(id, description, SeedListDescriptions) ?: description

@Composable
fun PacklyListEntry.displayItemNameSnapshot(): String = localizedSeedText(itemId.orEmpty(), itemNameSnapshot, SeedItemNames) ?: itemNameSnapshot

@Composable
fun TripEntry.displayNameSnapshot(): String = localizedSeedText(sourceItemId.orEmpty(), nameSnapshot, SeedItemNames) ?: nameSnapshot

@Composable
private fun localizedSeedText(id: String, currentValue: String, catalog: Map<String, SeedText>): String? {
    val seed = catalog[id] ?: return null
    // Preserve user-created or renamed text: only localize canonical seed snapshots.
    if (currentValue != seed.en) return null
    return seed.localized()
}

@Composable
private fun SeedText.localized(): String = when (LocalConfiguration.current.locales[0].supportedPacklyLocale().language) {
    "es" -> es
    "de" -> de
    else -> en
}

private fun Locale?.supportedPacklyLocale(): Locale = (this ?: Locale.getDefault()).supportedPacklyLocale()

private fun seedCategoryLabel(key: String, fallback: String): SeedText = SeedCategoryLabels[key] ?: SeedText(fallback, fallback, fallback)

private data class SeedText(val en: String, val es: String, val de: String)

private val SeedCategoryLabels = mapOf(
    "clothing" to SeedText("Clothing", "Ropa", "Kleidung"),
    "toiletries" to SeedText("Toiletries", "Artículos de aseo", "Körperpflege"),
    "electronics" to SeedText("Electronics", "Electrónica", "Elektronik"),
    "documents" to SeedText("Documents", "Documentos", "Dokumente"),
    "health" to SeedText("Health", "Salud", "Gesundheit"),
    "comfort" to SeedText("Comfort", "Comodidad", "Komfort"),
    "work" to SeedText("Work", "Trabajo", "Arbeit"),
    "food" to SeedText("Food", "Comida", "Essen"),
    "baby" to SeedText("Baby", "Bebé", "Baby"),
    "misc" to SeedText("Misc", "Varios", "Sonstiges"),
)

private val SeedItemNames = mapOf(
    "item_tshirts" to SeedText("T-shirts", "Camisetas", "T-Shirts"),
    "item_underwear" to SeedText("Underwear", "Ropa interior", "Unterwäsche"),
    "item_socks" to SeedText("Socks", "Calcetines", "Socken"),
    "item_pants" to SeedText("Pants / shorts", "Pantalones / shorts", "Hosen / Shorts"),
    "item_hoodie" to SeedText("Sweater or hoodie", "Suéter o sudadera", "Pullover oder Hoodie"),
    "item_sleepwear" to SeedText("Sleepwear", "Pijama", "Schlafkleidung"),
    "item_shoes" to SeedText("Comfortable shoes", "Zapatos cómodos", "Bequeme Schuhe"),
    "item_toothbrush" to SeedText("Toothbrush", "Cepillo de dientes", "Zahnbürste"),
    "item_toothpaste" to SeedText("Toothpaste", "Pasta de dientes", "Zahnpasta"),
    "item_shampoo" to SeedText("Shampoo", "Champú", "Shampoo"),
    "item_deodorant" to SeedText("Deodorant", "Desodorante", "Deodorant"),
    "item_hairbrush" to SeedText("Hairbrush / comb", "Cepillo / peine", "Haarbürste / Kamm"),
    "item_phone_charger" to SeedText("Phone charger", "Cargador del móvil", "Handy-Ladegerät"),
    "item_power_bank" to SeedText("Power bank", "Batería externa", "Powerbank"),
    "item_headphones" to SeedText("Headphones", "Auriculares", "Kopfhörer"),
    "item_laptop_charger" to SeedText("Laptop charger", "Cargador del portátil", "Laptop-Ladegerät"),
    "item_travel_adapter" to SeedText("Travel adapter", "Adaptador de viaje", "Reiseadapter"),
    "item_passport" to SeedText("Passport / ID", "Pasaporte / DNI", "Reisepass / Ausweis"),
    "item_tickets" to SeedText("Tickets / boarding pass", "Billetes / tarjeta de embarque", "Tickets / Bordkarte"),
    "item_wallet" to SeedText("Wallet", "Cartera", "Geldbörse"),
    "item_insurance" to SeedText("Travel insurance card", "Tarjeta de seguro de viaje", "Reiseversicherungskarte"),
    "item_medication" to SeedText("Medication", "Medicación", "Medikamente"),
    "item_sunscreen" to SeedText("Sunscreen", "Protector solar", "Sonnencreme"),
    "item_first_aid" to SeedText("First-aid basics", "Botiquín básico", "Erste-Hilfe-Basics"),
    "item_water_bottle" to SeedText("Water bottle", "Botella de agua", "Wasserflasche"),
    "item_book" to SeedText("Book / e-reader", "Libro / e-reader", "Buch / E-Reader"),
    "item_neck_pillow" to SeedText("Neck pillow", "Almohada de cuello", "Nackenkissen"),
)

private val SeedListNames = mapOf(
    "list_weekend" to SeedText("Weekend", "Fin de semana", "Wochenende"),
    "list_beach_day" to SeedText("Beach day", "Día de playa", "Strandtag"),
    "list_business" to SeedText("Business trip", "Viaje de negocios", "Geschäftsreise"),
    "list_camping" to SeedText("Camping", "Camping", "Camping"),
    "list_family" to SeedText("Family visit", "Visita familiar", "Familienbesuch"),
)

private val SeedListDescriptions = mapOf(
    "list_weekend" to SeedText("A light two-day starter list.", "Una lista ligera para dos días.", "Eine leichte Starterliste für zwei Tage."),
    "list_beach_day" to SeedText("Sunny-day essentials.", "Imprescindibles para un día soleado.", "Alles Wichtige für einen Sonnentag."),
    "list_business" to SeedText("Work travel basics.", "Básicos para viajes de trabajo.", "Grundausstattung für Geschäftsreisen."),
    "list_camping" to SeedText("Simple outdoors checklist.", "Lista sencilla para salir al aire libre.", "Einfache Outdoor-Checkliste."),
    "list_family" to SeedText("Friendly visit essentials.", "Imprescindibles para una visita familiar.", "Alles Wichtige für den Familienbesuch."),
)
