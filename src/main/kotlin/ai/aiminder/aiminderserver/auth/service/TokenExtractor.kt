package ai.aiminder.aiminderserver.auth.service

import org.springframework.http.server.reactive.ServerHttpRequest

interface TokenExtractor {
  fun extractAccessToken(request: ServerHttpRequest): String?

  fun extractRefreshToken(request: ServerHttpRequest): String?
}
