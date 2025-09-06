package ai.aiminder.aiminderserver.config

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class PostgresqlInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
  private val postgresqlContainer =
    PostgreSQLContainer(
      DockerImageName.parse("postgres:alpine"),
    ).withDatabaseName("aiminderdb")

  override fun initialize(applicationContext: ConfigurableApplicationContext) {
    postgresqlContainer.start()

    val url =
      "r2dbc:postgresql://" +
        "${postgresqlContainer.host}:${postgresqlContainer.firstMappedPort}/${postgresqlContainer.databaseName}"
    TestPropertyValues
      .of(
        mapOf<String, String>(
          "spring.r2dbc.url" to url,
          "spring.r2dbc.driver" to "postgresql",
          "spring.r2dbc.protocol" to "r2dbc",
          "spring.r2dbc.host" to postgresqlContainer.host,
          "spring.r2dbc.port" to postgresqlContainer.firstMappedPort.toString(),
          "spring.r2dbc.database" to postgresqlContainer.databaseName,
          "spring.r2dbc.username" to postgresqlContainer.username,
          "spring.r2dbc.password" to postgresqlContainer.password,
        ),
      ).applyTo(applicationContext)
  }
}
