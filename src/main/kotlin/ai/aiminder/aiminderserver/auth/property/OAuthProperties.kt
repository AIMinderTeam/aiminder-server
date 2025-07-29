package ai.aiminder.aiminderserver.auth.property

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

@ConfigurationProperties(prefix = "aiminder.oauth")
data class OAuthProperties(
    private val successUrl: String,
    private val errorUrl: String,
) {
    fun getSuccessUri(token: String): URI = URI.create("$successUrl?token=$token")

    fun getErrorUri(): URI = URI.create(errorUrl)
}
