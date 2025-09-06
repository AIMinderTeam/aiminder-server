package ai.aiminder.aiminderserver.auth.security

import ai.aiminder.aiminderserver.auth.property.SecurityProperties
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AllowedRedirectValidatorTest {
  private fun validator(): AllowedRedirectValidator =
    AllowedRedirectValidator(
      SecurityProperties(
        permitPaths = listOf(),
        allowOriginPatterns = listOf(),
        allowedRedirectHosts = listOf("localhost", "dev.aiminder.click"),
        defaultRedirectBaseUrl = "https://dev.aiminder.click",
      ),
    )

  @Test
  fun `allows http localhost`() {
    val v = validator()
    assertTrue(v.isAllowed("http://localhost:3000/cb"))
  }

  @Test
  fun `allows https dev`() {
    val v = validator()
    assertTrue(v.isAllowed("https://dev.aiminder.click/login/success"))
  }

  @Test
  fun `rejects disallowed host`() {
    val v = validator()
    assertFalse(v.isAllowed("https://evil.com"))
  }

  @Test
  fun `rejects non http scheme`() {
    val v = validator()
    assertFalse(v.isAllowed("javascript:alert(1)"))
  }
}
