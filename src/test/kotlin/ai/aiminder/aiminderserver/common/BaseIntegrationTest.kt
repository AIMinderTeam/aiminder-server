package ai.aiminder.aiminderserver.common

import ai.aiminder.aiminderserver.config.PostgresqlInitializer
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = [PostgresqlInitializer::class])
abstract class BaseIntegrationTest {
  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var databaseClient: DatabaseClient

  @Autowired
  private lateinit var environment: Environment

  private lateinit var flyway: Flyway

  @BeforeEach
  fun setupDatabase() {
    // Flyway 인스턴스를 수동으로 생성
    val username = environment.getProperty("spring.r2dbc.username")!!
    val password = environment.getProperty("spring.r2dbc.password")!!
    val host = environment.getProperty("spring.r2dbc.host")!!
    val port = environment.getProperty("spring.r2dbc.port")!!
    val database = environment.getProperty("spring.r2dbc.database")!!

    val jdbcUrl = "jdbc:postgresql://$host:$port/$database"

    flyway =
      Flyway
        .configure()
        .dataSource(jdbcUrl, username, password)
        .locations("classpath:db/migration")
        .cleanDisabled(false)
        .load()

    flyway.migrate()
  }

  @AfterEach
  fun cleanupDatabase() {
    flyway.clean()
  }
}
