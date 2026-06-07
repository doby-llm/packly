package com.dobyllm.packly.core.time

import com.dobyllm.packly.core.model.InstantString
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object PacklyDeadlineFormatter {
    private const val TimePattern = "HH:mm"

    const val InputPattern: String = "yyyy-MM-dd HH:mm"
    val DefaultPackByTime: LocalTime = LocalTime.of(18, 0)

    fun parseLocalInput(input: String, zoneId: ZoneId = ZoneId.systemDefault()): InstantString? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        return try {
            LocalDateTime.parse(trimmed, inputFormatter())
                .atZone(zoneId)
                .toInstant()
                .toString()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun formatInput(deadline: InstantString?, zoneId: ZoneId = ZoneId.systemDefault()): String =
        deadline.toInstantOrNull()
            ?.atZone(zoneId)
            ?.format(inputFormatter())
            .orEmpty()

    fun formatDisplay(deadline: InstantString?, zoneId: ZoneId = ZoneId.systemDefault()): String? =
        deadline.toInstantOrNull()
            ?.atZone(zoneId)
            ?.format(localizedFormatter(FormatterKind.Display))

    fun formatDate(deadline: InstantString?, zoneId: ZoneId = ZoneId.systemDefault()): String? =
        deadline.toInstantOrNull()
            ?.atZone(zoneId)
            ?.format(localizedFormatter(FormatterKind.Date))

    fun formatTime(deadline: InstantString?, zoneId: ZoneId = ZoneId.systemDefault()): String? =
        deadline.toInstantOrNull()
            ?.atZone(zoneId)
            ?.format(DateTimeFormatter.ofPattern(TimePattern, supportedDisplayLocale()))

    fun localDate(deadline: InstantString?, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate? =
        deadline.toInstantOrNull()?.atZone(zoneId)?.toLocalDate()

    fun localTime(deadline: InstantString?, zoneId: ZoneId = ZoneId.systemDefault()): LocalTime? =
        deadline.toInstantOrNull()?.atZone(zoneId)?.toLocalTime()?.withSecond(0)?.withNano(0)

    fun toInstantString(date: LocalDate, time: LocalTime = DefaultPackByTime, zoneId: ZoneId = ZoneId.systemDefault()): InstantString =
        LocalDateTime.of(date, time.withSecond(0).withNano(0))
            .atZone(zoneId)
            .toInstant()
            .toString()

    fun isCloseOrOverdue(deadline: InstantString?, now: Instant = Instant.now()): Boolean {
        val instant = deadline.toInstantOrNull() ?: return false
        return !instant.isAfter(now.plus(Duration.ofHours(24)))
    }

    private fun inputFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern(InputPattern, Locale.ROOT)

    private fun localizedFormatter(kind: FormatterKind): DateTimeFormatter {
        val locale = supportedDisplayLocale()
        return DateTimeFormatter.ofPattern(kind.patternFor(locale), locale)
    }

    private fun supportedDisplayLocale(locale: Locale = Locale.getDefault()): Locale =
        when (locale.language) {
            Locale.ENGLISH.language -> Locale.ENGLISH
            "es" -> Locale.forLanguageTag("es")
            Locale.GERMAN.language -> Locale.GERMAN
            else -> Locale.ENGLISH
        }

    private enum class FormatterKind {
        Display,
        Date;

        fun patternFor(locale: Locale): String = when (this) {
            Display -> when (locale.language) {
                "es" -> "EEE, d MMM, HH:mm"
                Locale.GERMAN.language -> "EEE, d. MMM, HH:mm"
                else -> "EEE, MMM d, HH:mm"
            }
            Date -> when (locale.language) {
                "es" -> "EEE, d MMM"
                Locale.GERMAN.language -> "EEE, d. MMM"
                else -> "EEE, MMM d"
            }
        }
    }
}

fun InstantString?.toInstantOrNull(): Instant? = this?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }
