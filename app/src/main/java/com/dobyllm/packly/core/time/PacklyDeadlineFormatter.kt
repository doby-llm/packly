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
    private val inputFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val displayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d 'at' HH:mm", Locale.getDefault())
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    const val InputPattern: String = "yyyy-MM-dd HH:mm"
    val DefaultPackByTime: LocalTime = LocalTime.of(18, 0)

    fun parseLocalInput(input: String, zoneId: ZoneId = ZoneId.systemDefault()): InstantString? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        return try {
            LocalDateTime.parse(trimmed, inputFormatter)
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
            ?.format(inputFormatter)
            .orEmpty()

    fun formatDisplay(deadline: InstantString?, zoneId: ZoneId = ZoneId.systemDefault()): String? =
        deadline.toInstantOrNull()
            ?.atZone(zoneId)
            ?.format(displayFormatter)

    fun formatDate(deadline: InstantString?, zoneId: ZoneId = ZoneId.systemDefault()): String? =
        deadline.toInstantOrNull()
            ?.atZone(zoneId)
            ?.format(dateFormatter)

    fun formatTime(deadline: InstantString?, zoneId: ZoneId = ZoneId.systemDefault()): String? =
        deadline.toInstantOrNull()
            ?.atZone(zoneId)
            ?.format(timeFormatter)

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
}

fun InstantString?.toInstantOrNull(): Instant? = this?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }
