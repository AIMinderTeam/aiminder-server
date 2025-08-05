package ai.aiminder.aiminderserver.common.util

import java.util.UUID

fun String.toUUID(): UUID =
  runCatching {
    UUID.fromString(this)
  }.getOrElse {
    throw IllegalArgumentException("Invalid UUID string: $this")
  }