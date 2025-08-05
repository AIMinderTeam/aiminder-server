package ai.aiminder.aiminderserver.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/oauth2/authorization")
@Tag(name = "OAuth2 Login", description = "OAuth2 소셜 로그인 API")
class OAuth2Controller {
  @GetMapping("/google")
  @Operation(
    summary = "Google OAuth2 로그인",
    description =
      "Google OAuth2를 통한 소셜 로그인을 합니다. " +
        "이 엔드포인트는 Google 인증 페이지로 리다이렉트됩니다.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "302", description = "Google 인증 페이지로 리다이렉트"),
      ApiResponse(responseCode = "500", description = "서버 오류"),
    ],
  )
  fun googleLogin(): ResponseEntity<Unit> = ResponseEntity.status(HttpStatus.FOUND).build()

  @GetMapping("/kakao")
  @Operation(
    summary = "Kakao OAuth2 로그인",
    description =
      "Kakao OAuth2를 통합 소셜 로그인을 합니다. " +
        "이 엔드포인트는 Kakao 인증 페이지로 리다이렉트됩니다.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "302", description = "Kakao 인증 페이지로 리다이렉트"),
      ApiResponse(responseCode = "500", description = "서버 오류"),
    ],
  )
  fun kakaoLogin(): ResponseEntity<Unit> = ResponseEntity.status(HttpStatus.FOUND).build()
}