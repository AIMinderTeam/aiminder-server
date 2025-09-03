package ai.aiminder.aiminderserver.common.error

import org.springframework.http.HttpStatusCode

interface ServiceError {
  val mainCode: String
  val subCode: String
  val message: String
  val statusCode: HttpStatusCode

  fun toCode() = "$mainCode:$subCode"
}
