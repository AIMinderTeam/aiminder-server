package ai.aiminder.aiminderserver.common.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

fun String.toUUID(): UUID =
  runCatching {
    UUID.fromString(this)
  }.getOrElse {
    throw IllegalArgumentException("Invalid UUID string: $this")
  }

fun LocalDateTime.toUtcInstant(): Instant = this.toInstant(ZoneOffset.systemDefault().rules.getOffset(this))
