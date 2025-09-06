package ai.aiminder.aiminderserver.common.error

import org.springframework.http.HttpStatus

abstract class ServiceError : RuntimeException() {
  abstract val mainCode: String
  abstract val status: HttpStatus
  abstract override val message: String

  val code
    get() = "$mainCode:${this::class.simpleName?.uppercase() ?: "UNKNOWN"}"
}
