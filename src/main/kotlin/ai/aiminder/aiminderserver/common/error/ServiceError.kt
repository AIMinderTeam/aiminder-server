package ai.aiminder.aiminderserver.common.error

interface ServiceError {
  val mainCode: String
  val subCode: String
  val message: String

  fun toCode() = "$mainCode:$subCode"
}