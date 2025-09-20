package ai.aiminder.aiminderserver.common

import ai.aiminder.aiminderserver.common.util.logger
import ai.aiminder.aiminderserver.config.PostgresqlInitializer
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import java.io.File
import java.io.IOException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = [PostgresqlInitializer::class])
abstract class BaseIntegrationTest {
  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  private lateinit var environment: Environment

  private lateinit var flyway: Flyway

  @BeforeEach
  fun setupDatabase() {
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

  companion object {
    private val logger = logger()
    private const val UPLOAD_DIR: String = "./uploads"

    init {
      Runtime.getRuntime().addShutdownHook(
        Thread {
          cleanupUploadDirectory()
        },
      )
    }

    private fun cleanupUploadDirectory() {
      UPLOAD_DIR.let { dir ->
        try {
          val uploadDirFile = File(dir)
          if (uploadDirFile.exists()) {
            uploadDirFile.deleteRecursively()
            logger.info("업로드 디렉터리 정리 완료: $dir")
          }
        } catch (e: IOException) {
          logger.warn("업로드 디렉터리 정리 중 IO 오류 발생", e)
        } catch (e: SecurityException) {
          logger.warn("업로드 디렉터리 정리 권한 오류", e)
        } catch (e: Exception) {
          logger.error("업로드 디렉터리 정리 중 알 수 없는 오류 발생", e)
        }
      }
    }
  }
}
