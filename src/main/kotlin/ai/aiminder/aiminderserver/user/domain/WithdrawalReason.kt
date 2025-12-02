package ai.aiminder.aiminderserver.user.domain

enum class WithdrawalReason(val displayName: String) {
  SERVICE_DISSATISFACTION("서비스가 마음에 들지 않음"),
  USING_OTHER_SERVICE("다른 서비스를 사용하게 됨"),
  PRIVACY_CONCERN("개인정보 보호 우려"),
  LOW_USAGE_FREQUENCY("사용 빈도가 낮음"),
  OTHER("기타"),
  ;

  companion object {
    fun fromDisplayName(displayName: String): WithdrawalReason? {
      return entries.find { it.displayName == displayName }
    }
  }
}
