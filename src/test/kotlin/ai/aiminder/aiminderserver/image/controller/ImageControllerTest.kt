package ai.aiminder.aiminderserver.image.controller

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.image.dto.ImageResponse
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.entity.UserEntity
import ai.aiminder.aiminderserver.user.repository.UserRepository
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

class ImageControllerTest
  @Autowired
  constructor(
    private val userRepository: UserRepository,
  ) : BaseIntegrationTest() {
    private lateinit var testUser: User
    private lateinit var authentication: UsernamePasswordAuthenticationToken

    @BeforeEach
    fun setUp() =
      runTest {
        val savedUser =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.GOOGLE,
              providerId = "test-provider-123",
            ),
          )
        testUser = User.from(savedUser)
        authentication =
          UsernamePasswordAuthenticationToken(
            testUser,
            null,
            listOf(SimpleGrantedAuthority(Role.USER.name)),
          )
      }

    @Test
    fun `이미지 업로드 성공 테스트`() {
      // given
      val bodyBuilder = MultipartBodyBuilder()
      bodyBuilder
        .part("file", ClassPathResource("test-image.jpg"))
        .contentType(MediaType.IMAGE_JPEG)

      // when
      val response =
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/images")
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<ImageResponse>>()
          .returnResult()
          .responseBody!!

      // then
      response.data?.also {
        assertThat(it.id).isNotNull
        assertThat(it.userId).isEqualTo(testUser.id)
        assertThat(it.originalFileName).isEqualTo("test-image.jpg")
        assertThat(it.contentType).isEqualTo("image/jpeg")
        assertThat(it.fileSize).isGreaterThan(0)
        assertThat(it.deletedAt).isNull()
      }
    }

    @Test
    fun `인증되지 않은 사용자 이미지 업로드 시 401 반환`() {
      // given
      val bodyBuilder = MultipartBodyBuilder()
      bodyBuilder
        .part("file", ClassPathResource("test-image.jpg"))
        .contentType(MediaType.IMAGE_JPEG)

      // when
      val response =
        webTestClient
          .post()
          .uri("/api/v1/images")
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
          .exchange()
          .expectStatus()
          .isUnauthorized
          .expectBody<ServiceResponse<Unit>>()
          .returnResult()
          .responseBody!!

      // then
      assertThat(response.statusCode).isEqualTo(401)
      assertThat(response.message).isEqualTo("인증이 필요합니다. 로그인을 진행해주세요.")
      assertThat(response.errorCode).isEqualTo("AUTH:UNAUTHORIZED")
    }
  }
