package ai.aiminder.aiminderserver.assistant.config

import ai.aiminder.aiminderserver.common.property.DatabaseProperties
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.ChatMemoryRepository
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@Configuration
class ChatMemoryConfig(
  private val databaseProperties: DatabaseProperties,
) {
  @Bean
  fun jdbcDataSource(): DataSource {
    val config = HikariConfig()
    val jdbcUrl = databaseProperties.url.replace("r2dbc:postgresql://", "jdbc:postgresql://")
    config.jdbcUrl = jdbcUrl
    config.username = databaseProperties.username
    config.password = databaseProperties.password
    config.driverClassName = "org.postgresql.Driver"
    return HikariDataSource(config)
  }

  @Bean
  fun jdbcTemplate(dataSource: DataSource): JdbcTemplate = JdbcTemplate(dataSource)

  @Bean
  fun chatMemoryRepository(jdbcTemplate: JdbcTemplate): ChatMemoryRepository =
    JdbcChatMemoryRepository
      .builder()
      .jdbcTemplate(jdbcTemplate)
      .dialect(PostgresChatMemoryRepositoryDialect())
      .build()

  @Bean
  fun chatMemory(chatMemoryRepository: ChatMemoryRepository): ChatMemory =
    MessageWindowChatMemory
      .builder()
      .chatMemoryRepository(chatMemoryRepository)
      .maxMessages(20)
      .build()
}
