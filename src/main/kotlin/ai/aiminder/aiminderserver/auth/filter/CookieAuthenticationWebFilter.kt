package ai.aiminder.aiminderserver.auth.filter

import ai.aiminder.aiminderserver.auth.domain.AccessToken
import ai.aiminder.aiminderserver.auth.domain.RefreshToken
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.auth.property.CookieProperties
import ai.aiminder.aiminderserver.auth.service.CookieManager
import ai.aiminder.aiminderserver.auth.service.TokenService
import ai.aiminder.aiminderserver.common.util.logger
import ai.aiminder.aiminderserver.common.util.toUUID
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.service.UserService
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class CookieAuthenticationWebFilter(
  @Qualifier("accessJwtDecoder") private val accessDecoder: ReactiveJwtDecoder,
  @Qualifier("refreshJwtDecoder") private val refreshDecoder: ReactiveJwtDecoder,
  private val tokenService: TokenService,
  private val userService: UserService,
  private val cookieProperties: CookieProperties,
  private val cookieManager: CookieManager,
) : WebFilter {
  private val logger = logger()

  private companion object {
    private const val ACCESS_COOKIE = "ACCESS_TOKEN"
    private const val REFRESH_COOKIE = "REFRESH_TOKEN"
  }

  override fun filter(
    exchange: ServerWebExchange,
    chain: WebFilterChain,
  ): Mono<Void> {
    val request: ServerHttpRequest = exchange.request
    val response: ServerHttpResponse = exchange.response

    logger.debug("CookieAuth start method=${request.method.name()} path=${request.uri.path} id=${request.id}")

    val accessToken: AccessToken? = extractCookie(request, ACCESS_COOKIE)
    val refreshToken: RefreshToken? = extractCookie(request, REFRESH_COOKIE)

    logHeader("access", accessToken)
    logHeader("refresh", refreshToken)

    logger.debug(
      "CookieAuth cookies present: access=${!accessToken.isNullOrBlank()} refresh=${!refreshToken.isNullOrBlank()} " +
        "id=${request.id}",
    )

    if (accessToken.isNullOrBlank()) {
      logger.debug("CookieAuth no access token; skipping auth id=${request.id}")
      return chain.filter(exchange)
    }

    return accessDecoder
      .decode(accessToken)
      .flatMap { jwt ->
        logger.debug("CookieAuth access decoded: sub={} exp={} id={}", jwt.subject, jwt.expiresAt, request.id)
        val token: AccessToken = accessToken
        if (!tokenService.validateAccessToken(token)) {
          logger.debug("CookieAuth access token invalid for sub=${jwt.subject} id=${request.id}")
          return@flatMap Mono.error(AuthError.InvalidAccessToken())
        }
        logger.debug("CookieAuth access token valid for sub=${jwt.subject} id=${request.id}")
        processAuthentication(jwt, chain, exchange)
      }.onErrorResume { err ->
        if (err is AuthError.InvalidAccessToken || err is JwtException) {
          logger.debug(
            "CookieAuth error on access validation id=${request.id} path=${request.uri.path}: ${err.message}",
            err,
          )
          if (refreshToken.isNullOrBlank()) {
            logger.debug("CookieAuth no refresh token; proceeding unauthenticated id=${request.id}")
            return@onErrorResume chain.filter(exchange)
          }
          logger.debug("CookieAuth attempting refresh flow id=${request.id}")
          refreshDecoder
            .decode(refreshToken)
            .flatMap {
              mono {
                val token: RefreshToken = refreshToken
                logger.debug("CookieAuth decoding refresh token succeeded id=${request.id}")
                if (!tokenService.validateRefreshToken(token)) {
                  logger.debug("CookieAuth refresh token invalid id=${request.id}")
                  throw AuthError.InvalidRefreshToken()
                }
                val user = userService.getUser(token)
                logger.debug("CookieAuth refresh validated; issuing new tokens userId={} id={}", user.id, request.id)
                val newAccess = tokenService.createAccessToken(user)
                val newRefresh = tokenService.createRefreshToken(user)
                Pair(newAccess, newRefresh)
              }.flatMap { (newAccess, newRefresh) ->
                accessDecoder.decode(newAccess).flatMap { newJwt ->
                  cookieManager.setTokenCookies(response, newAccess, newRefresh)
                  logger.info("CookieAuth reissued tokens; cookies set id=${request.id}")
                  logger.debug(
                    "CookieAuth cookie flags domain=${cookieProperties.domain} sameSite=${cookieProperties.sameSite} " +
                      "secure=${cookieProperties.secure} id=${request.id}",
                  )
                  processAuthentication(newJwt, chain, exchange)
                }
              }
            }.onErrorResume {
              if (it is AuthError || it is JwtException) {
                logger.debug(
                  "CookieAuth error during refresh flow id=${request.id} path=${request.uri.path}: ${it.message}",
                  it,
                )
                cookieManager.clearTokenCookies(response)
              }
              chain.filter(exchange)
            }
        } else {
          chain.filter(exchange)
        }
      }
  }

  private fun processAuthentication(
    jwt: Jwt,
    chain: WebFilterChain,
    exchange: ServerWebExchange,
  ): Mono<Void> {
    val requestId = exchange.request.id
    val userId =
      runCatching { jwt.subject.toUUID() }
        .getOrElse {
          logger.error("CookieAuth invalid subject on JWT; sub='${jwt.subject}' id=$requestId", it)
          return chain.filter(exchange)
        }

    return mono { User.from(userService.getUserById(userId)) }
      .flatMap { user ->
        val authorities = listOf(SimpleGrantedAuthority(Role.USER.name))
        val authentication = UsernamePasswordAuthenticationToken(user, null, authorities).apply { details = jwt }
        val securityContext = SecurityContextImpl(authentication)
        logger.debug(
          "CookieAuth setting SecurityContext sub={} authorities={} id={}",
          jwt.subject,
          authentication.authorities,
          requestId,
        )
        chain
          .filter(exchange)
          .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
      }
  }

  private fun extractCookie(
    request: ServerHttpRequest,
    name: String,
  ): String? {
    request.cookies
      .getFirst(name)
      ?.value
      ?.let {
        logger.debug("CookieAuth extractCookie name=$name found=true (from cookies) id=${request.id}")
        return it
      }
    val raw = request.headers.getFirst("Cookie") ?: request.headers.getFirst("COOKIE")
    if (raw.isNullOrBlank()) return null
    val value =
      raw
        .split(";")
        .map { it.trim() }
        .firstOrNull { it.startsWith("$name=") }
        ?.substringAfter('=')
    logger.debug("CookieAuth extractCookie name=$name found=${!value.isNullOrBlank()} (from header) id=${request.id}")
    return value
  }

  fun logHeader(
    name: String,
    token: String?,
  ) {
    if (token.isNullOrBlank()) return
    runCatching {
      val parts = token.split('.')
      if (parts.size >= 2) {
        val headerJson =
          String(
            java.util.Base64
              .getUrlDecoder()
              .decode(parts[0]),
          )
        val alg = Regex("\"alg\"\\s*:\\s*\"([^\"]+)\"").find(headerJson)?.groupValues?.getOrNull(1)
        val kid = Regex("\"kid\"\\s*:\\s*\"([^\"]+)\"").find(headerJson)?.groupValues?.getOrNull(1)
        logger.debug("CookieAuth $name header alg=${alg ?: "?"} kid=${kid ?: "-"}")
      }
    }.onFailure {
      logger.debug("CookieAuth $name header error parsing token: ${it.message}")
    }
  }
}
