package ai.aiminder.aiminderserver.auth.filter

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.auth.property.CookieProperties
import ai.aiminder.aiminderserver.auth.service.CookieManager
import ai.aiminder.aiminderserver.auth.service.DefaultCookieManager
import ai.aiminder.aiminderserver.auth.service.TokenService
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.entity.UserEntity
import ai.aiminder.aiminderserver.user.service.UserService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CookieAuthenticationWebFilterTest {
  private val accessDecoder: ReactiveJwtDecoder = mockk()
  private val refreshDecoder: ReactiveJwtDecoder = mockk()
  private val tokenService: TokenService = mockk()
  private val userService: UserService = mockk()
  private val cookieProps = CookieProperties(domain = "", sameSite = "Lax", httpOnly = true, secure = false)
  private val cookieManager: CookieManager = DefaultCookieManager(cookieProps)

  private fun jwt(token: String): Jwt =
    Jwt
      .withTokenValue(token)
      .header("alg", "none")
      .claim("sub", "00000000-0000-0000-0000-000000000000")
      .build()

  @Test
  fun `유효한 액세스 토큰으로 인증 컨텍스트가 설정된다`() {
    // given
    val access = "aaa.bbb.ccc"
    every { accessDecoder.decode(access) } returns Mono.just(jwt(access))
    every { tokenService.validateAccessToken(access) } returns true
    val subjectId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val user = UserEntity(id = subjectId, provider = OAuth2Provider.GOOGLE, providerId = "123")
    coEvery { userService.getUserById(subjectId) } returns user

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=$access")
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticated = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder
          .getContext()
          .doOnNext { authenticated = true }
          .then()
      }

    val filter =
      CookieAuthenticationWebFilter(
        accessDecoder = accessDecoder,
        refreshDecoder = refreshDecoder,
        tokenService = tokenService,
        userService = userService,
        cookieProperties = cookieProps,
        cookieManager = cookieManager,
      )

    // when
    filter.filter(exchange, chain).block()

    // then
    assertEquals(true, authenticated)
  }

  @Test
  fun `액세스 토큰 무효, 리프래시 유효 시 재발급되고 응답 쿠키가 설정된다`() {
    // given
    val access = "bad.access"
    val refresh = "good.refresh"
    val newAccess = "new.access"
    val newRefresh = "new.refresh"

    every { accessDecoder.decode(access) } returns Mono.error(JwtException("invalid"))
    every { refreshDecoder.decode(refresh) } returns Mono.just(jwt(refresh))
    coEvery { tokenService.validateRefreshToken(refresh) } returns true

    val subjectId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val createdAt = Instant.now()
    val user =
      User(
        id = subjectId,
        provider = OAuth2Provider.GOOGLE,
        providerId = "123",
        createdAt = createdAt,
        updatedAt = createdAt,
      )
    coEvery { userService.getUser(refresh) } returns user
    every { tokenService.createAccessToken(user) } returns newAccess
    coEvery { tokenService.createRefreshToken(user) } returns newRefresh
    every { accessDecoder.decode(newAccess) } returns Mono.just(jwt(newAccess))
    coEvery { userService.getUserById(subjectId) } returns
      UserEntity(
        id = user.id,
        provider = user.provider,
        providerId = user.providerId,
        createdAt = user.createdAt,
        updatedAt = user.updatedAt,
      )

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=$access; REFRESH_TOKEN=$refresh")
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticated = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder
          .getContext()
          .doOnNext { authenticated = true }
          .then()
      }

    val filter =
      CookieAuthenticationWebFilter(
        accessDecoder,
        refreshDecoder,
        tokenService,
        userService,
        cookieProps,
        cookieManager,
      )

    // when
    filter.filter(exchange, chain).block()

    // then
    val accessCookie = exchange.response.cookies["ACCESS_TOKEN"]?.firstOrNull()
    val refreshCookie = exchange.response.cookies["REFRESH_TOKEN"]?.firstOrNull()
    assertNotNull(accessCookie)
    assertNotNull(refreshCookie)
    assertEquals(newAccess, accessCookie.value)
    assertEquals(newRefresh, refreshCookie.value)
    assertEquals(true, authenticated)
  }

  @Test
  fun `액세스 무효, 리프래시 무효 시 만료 쿠키로 제거된다`() {
    // given
    val access = "bad.access"
    val refresh = "bad.refresh"
    every { accessDecoder.decode(access) } returns Mono.error(JwtException("invalid"))
    every { refreshDecoder.decode(refresh) } returns Mono.error(JwtException("invalid"))

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=$access; REFRESH_TOKEN=$refresh")
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticationSet = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder
          .getContext()
          .doOnNext { authenticationSet = true }
          .then()
      }

    val filter =
      CookieAuthenticationWebFilter(
        accessDecoder,
        refreshDecoder,
        tokenService,
        userService,
        cookieProps,
        cookieManager,
      )

    // when
    filter.filter(exchange, chain).block()

    // then
    val accessCookie = exchange.response.cookies["ACCESS_TOKEN"]?.firstOrNull()
    val refreshCookie = exchange.response.cookies["REFRESH_TOKEN"]?.firstOrNull()
    assertNotNull(accessCookie)
    assertNotNull(refreshCookie)
    assertEquals(0, accessCookie.maxAge.seconds)
    assertEquals(0, refreshCookie.maxAge.seconds)
    assertEquals(false, authenticationSet)
  }

  @Test
  fun `쿠키 미제공 시 체인만 통과한다`() {
    // given
    val request = MockServerHttpRequest.get("/api/test").build()
    val exchange = MockServerWebExchange.from(request)

    var authenticationSet = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder
          .getContext()
          .doOnNext { authenticationSet = true }
          .then()
      }

    val filter =
      CookieAuthenticationWebFilter(
        accessDecoder,
        refreshDecoder,
        tokenService,
        userService,
        cookieProps,
        cookieManager,
      )

    // when
    filter.filter(exchange, chain).block()

    // then
    assertEquals(false, authenticationSet)
  }

  @Test
  fun `액세스 토큰 디코딩 성공하지만 검증 실패하는 경우`() {
    // given
    val access = "valid.jwt.format"
    val refresh = "good.refresh"
    every { accessDecoder.decode(access) } returns Mono.just(jwt(access))
    every { tokenService.validateAccessToken(access) } returns false // 검증 실패
    every { refreshDecoder.decode(refresh) } returns Mono.just(jwt(refresh))
    coEvery { tokenService.validateRefreshToken(refresh) } returns true

    val subjectId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val createdAt = Instant.now()
    val user =
      User(
        id = subjectId,
        provider = OAuth2Provider.GOOGLE,
        providerId = "123",
        createdAt = createdAt,
        updatedAt = createdAt,
      )
    val newAccess = "new.access"
    val newRefresh = "new.refresh"

    coEvery { userService.getUser(refresh) } returns user
    every { tokenService.createAccessToken(user) } returns newAccess
    coEvery { tokenService.createRefreshToken(user) } returns newRefresh
    every { accessDecoder.decode(newAccess) } returns Mono.just(jwt(newAccess))
    coEvery { userService.getUserById(subjectId) } returns
      UserEntity(id = user.id, provider = user.provider, providerId = user.providerId)

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=$access; REFRESH_TOKEN=$refresh")
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticated = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder.getContext().doOnNext { authenticated = true }.then()
      }

    val filter =
      CookieAuthenticationWebFilter(
        accessDecoder,
        refreshDecoder,
        tokenService,
        userService,
        cookieProps,
        cookieManager,
      )

    // when
    filter.filter(exchange, chain).block()

    // then
    val accessCookie = exchange.response.cookies["ACCESS_TOKEN"]?.firstOrNull()
    val refreshCookie = exchange.response.cookies["REFRESH_TOKEN"]?.firstOrNull()
    assertNotNull(accessCookie)
    assertNotNull(refreshCookie)
    assertEquals(newAccess, accessCookie.value)
    assertEquals(newRefresh, refreshCookie.value)
    assertEquals(true, authenticated)
  }

  @Test
  fun `액세스 토큰 무효하고 리프래시 토큰도 없는 경우`() {
    // given
    val access = "bad.access"
    every { accessDecoder.decode(access) } returns Mono.error(JwtException("invalid"))

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=$access")
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticationSet = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder.getContext().doOnNext { authenticationSet = true }.then()
      }

    val filter =
      CookieAuthenticationWebFilter(
        accessDecoder,
        refreshDecoder,
        tokenService,
        userService,
        cookieProps,
        cookieManager,
      )

    // when
    filter.filter(exchange, chain).block()

    // then
    assertEquals(false, authenticationSet)
    assertEquals(0, exchange.response.cookies.size) // 쿠키 설정되지 않음
  }

  @Test
  fun `리프래시 토큰 디코딩 성공하지만 검증 실패하는 경우`() {
    // given
    val access = "bad.access"
    val refresh = "valid.jwt.format"
    every { accessDecoder.decode(access) } returns Mono.error(JwtException("invalid"))
    every { refreshDecoder.decode(refresh) } returns Mono.just(jwt(refresh))
    coEvery { tokenService.validateRefreshToken(refresh) } returns false // 검증 실패

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=$access; REFRESH_TOKEN=$refresh")
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticationSet = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder.getContext().doOnNext { authenticationSet = true }.then()
      }

    val filter =
      CookieAuthenticationWebFilter(
        accessDecoder,
        refreshDecoder,
        tokenService,
        userService,
        cookieProps,
        cookieManager,
      )

    // when
    filter.filter(exchange, chain).block()

    // then
    val accessCookie = exchange.response.cookies["ACCESS_TOKEN"]?.firstOrNull()
    val refreshCookie = exchange.response.cookies["REFRESH_TOKEN"]?.firstOrNull()
    assertNotNull(accessCookie)
    assertNotNull(refreshCookie)
    assertEquals(0, accessCookie.maxAge.seconds)
    assertEquals(0, refreshCookie.maxAge.seconds)
    assertEquals(false, authenticationSet)
  }

  @Test
  fun `리프래시 플로우에서 사용자 조회 실패하는 경우`() {
    // given
    val access = "bad.access"
    val refresh = "good.refresh"
    every { accessDecoder.decode(access) } returns Mono.error(JwtException("invalid"))
    every { refreshDecoder.decode(refresh) } returns Mono.just(jwt(refresh))
    coEvery { tokenService.validateRefreshToken(refresh) } returns true
    val subjectId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    coEvery { userService.getUser(refresh) } throws AuthError.UserNotFoundException(subjectId)

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=$access; REFRESH_TOKEN=$refresh")
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticationSet = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder.getContext().doOnNext { authenticationSet = true }.then()
      }

    val filter =
      CookieAuthenticationWebFilter(
        accessDecoder,
        refreshDecoder,
        tokenService,
        userService,
        cookieProps,
        cookieManager,
      )

    // when
    filter.filter(exchange, chain).block()

    // then
    val accessCookie = exchange.response.cookies["ACCESS_TOKEN"]?.firstOrNull()
    val refreshCookie = exchange.response.cookies["REFRESH_TOKEN"]?.firstOrNull()
    assertNotNull(accessCookie)
    assertNotNull(refreshCookie)
    assertEquals(0, accessCookie.maxAge.seconds)
    assertEquals(0, refreshCookie.maxAge.seconds)
    assertEquals(false, authenticationSet)
  }

  @Test
  fun `인증 처리에서 사용자 조회 실패하는 경우`() {
    // given
    val access = "aaa.bbb.ccc"
    every { accessDecoder.decode(access) } returns Mono.just(jwt(access))
    every { tokenService.validateAccessToken(access) } returns true
    val subjectId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    coEvery { userService.getUserById(subjectId) } throws AuthError.UserNotFoundException(subjectId)

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=$access")
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticationSet = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder.getContext().doOnNext { authenticationSet = true }.then()
      }

    val filter =
      CookieAuthenticationWebFilter(
        accessDecoder,
        refreshDecoder,
        tokenService,
        userService,
        cookieProps,
        cookieManager,
      )

    // when
    filter.filter(exchange, chain).block()

    // then
    assertEquals(false, authenticationSet)
  }

  @Test
  fun `JWT subject가 유효한 UUID가 아닌 경우`() {
    // given
    val access = "aaa.bbb.ccc"
    val invalidJwt =
      Jwt
        .withTokenValue(access)
        .header("alg", "none")
        .claim("sub", "invalid-uuid-format")
        .build()

    every { accessDecoder.decode(access) } returns Mono.just(invalidJwt)
    every { tokenService.validateAccessToken(access) } returns true

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=$access")
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticationSet = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder.getContext().doOnNext { authenticationSet = true }.then()
      }

    val filter =
      CookieAuthenticationWebFilter(
        accessDecoder,
        refreshDecoder,
        tokenService,
        userService,
        cookieProps,
        cookieManager,
      )

    // when
    filter.filter(exchange, chain).block()

    // then
    assertEquals(false, authenticationSet)
  }

  @Test
  fun `새로운 액세스 토큰 디코딩 실패하는 경우`() {
    // given
    val access = "bad.access"
    val refresh = "good.refresh"
    val newAccess = "corrupted.access"
    val newRefresh = "new.refresh"

    every { accessDecoder.decode(access) } returns Mono.error(JwtException("invalid"))
    every { refreshDecoder.decode(refresh) } returns Mono.just(jwt(refresh))
    coEvery { tokenService.validateRefreshToken(refresh) } returns true

    val subjectId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val createdAt = Instant.now()
    val user =
      User(
        id = subjectId,
        provider = OAuth2Provider.GOOGLE,
        providerId = "123",
        createdAt = createdAt,
        updatedAt = createdAt,
      )

    coEvery { userService.getUser(refresh) } returns user
    every { tokenService.createAccessToken(user) } returns newAccess
    coEvery { tokenService.createRefreshToken(user) } returns newRefresh
    every { accessDecoder.decode(newAccess) } returns Mono.error(JwtException("corrupted token"))

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=$access; REFRESH_TOKEN=$refresh")
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticationSet = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder.getContext().doOnNext { authenticationSet = true }.then()
      }

    val filter =
      CookieAuthenticationWebFilter(
        accessDecoder,
        refreshDecoder,
        tokenService,
        userService,
        cookieProps,
        cookieManager,
      )

    // when
    filter.filter(exchange, chain).block()

    // then
    val accessCookie = exchange.response.cookies["ACCESS_TOKEN"]?.firstOrNull()
    val refreshCookie = exchange.response.cookies["REFRESH_TOKEN"]?.firstOrNull()
    assertNotNull(accessCookie)
    assertNotNull(refreshCookie)
    assertEquals(0, accessCookie.maxAge.seconds)
    assertEquals(0, refreshCookie.maxAge.seconds)
    assertEquals(false, authenticationSet)
  }

  @Test
  fun `쿠키 추출 기능 - request cookies에서 추출하는 경우`() {
    // given
    val access = "aaa.bbb.ccc"
    every { accessDecoder.decode(access) } returns Mono.just(jwt(access))
    every { tokenService.validateAccessToken(access) } returns true
    val subjectId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val user = UserEntity(id = subjectId, provider = OAuth2Provider.GOOGLE, providerId = "123")
    coEvery { userService.getUserById(subjectId) } returns user

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .cookie(HttpCookie("ACCESS_TOKEN", access)) // request.cookies로 설정
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticated = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder.getContext().doOnNext { authenticated = true }.then()
      }

    val filter =
      CookieAuthenticationWebFilter(
        accessDecoder,
        refreshDecoder,
        tokenService,
        userService,
        cookieProps,
        cookieManager,
      )

    // when
    filter.filter(exchange, chain).block()

    // then
    assertEquals(true, authenticated)
  }

  @Test
  fun `쿠키 추출 기능 - Cookie 헤더에서 파싱하는 경우`() {
    // given
    val access = "aaa.bbb.ccc"
    every { accessDecoder.decode(access) } returns Mono.just(jwt(access))
    every { tokenService.validateAccessToken(access) } returns true
    val subjectId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val user = UserEntity(id = subjectId, provider = OAuth2Provider.GOOGLE, providerId = "123")
    coEvery { userService.getUserById(subjectId) } returns user

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "OTHER_COOKIE=value; ACCESS_TOKEN=$access; ANOTHER=test")
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticated = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder.getContext().doOnNext { authenticated = true }.then()
      }

    val filter =
      CookieAuthenticationWebFilter(
        accessDecoder,
        refreshDecoder,
        tokenService,
        userService,
        cookieProps,
        cookieManager,
      )

    // when
    filter.filter(exchange, chain).block()

    // then
    assertEquals(true, authenticated)
  }

  @Test
  fun `만료 쿠키 생성 기능 테스트`() {
    // given
    val access = "bad.access"
    val refresh = "bad.refresh"
    every { accessDecoder.decode(access) } returns Mono.error(JwtException("invalid"))
    every { refreshDecoder.decode(refresh) } returns Mono.error(JwtException("invalid"))

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=$access; REFRESH_TOKEN=$refresh")
        .build()
    val exchange = MockServerWebExchange.from(request)

    val chain = WebFilterChain { _ -> Mono.empty() }
    val filter =
      CookieAuthenticationWebFilter(
        accessDecoder,
        refreshDecoder,
        tokenService,
        userService,
        cookieProps,
        cookieManager,
      )

    // when
    filter.filter(exchange, chain).block()

    // then
    val accessCookie = exchange.response.cookies["ACCESS_TOKEN"]?.firstOrNull()
    val refreshCookie = exchange.response.cookies["REFRESH_TOKEN"]?.firstOrNull()

    assertNotNull(accessCookie)
    assertNotNull(refreshCookie)

    // 만료 쿠키 속성 확인
    assertEquals("", accessCookie.value)
    assertEquals("", refreshCookie.value)
    assertEquals(0, accessCookie.maxAge.seconds)
    assertEquals(0, refreshCookie.maxAge.seconds)
    assertEquals("/", accessCookie.path)
    assertEquals("/", refreshCookie.path)
    assertEquals("Lax", accessCookie.sameSite)
    assertEquals("Lax", refreshCookie.sameSite)
    assertEquals(true, accessCookie.isHttpOnly)
    assertEquals(true, refreshCookie.isHttpOnly)
    assertEquals(false, accessCookie.isSecure)
    assertEquals(false, refreshCookie.isSecure)
  }
}
