package ai.aiminder.aiminderserver.common.config

import ai.aiminder.aiminderserver.common.property.DatabaseProperties
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer

@Configuration
@EnableR2dbcRepositories
class R2dbcConfiguration(
  private val databaseProperties: DatabaseProperties,
  private val customConverters: List<Converter<*, *>>,
) : AbstractR2dbcConfiguration() {
  override fun connectionFactory(): ConnectionFactory =
    ConnectionFactories.get(
      ConnectionFactoryOptions
        .builder()
        .option(ConnectionFactoryOptions.DRIVER, databaseProperties.driver)
        .option(ConnectionFactoryOptions.PROTOCOL, databaseProperties.protocol)
        .option(ConnectionFactoryOptions.HOST, databaseProperties.host)
        .option(ConnectionFactoryOptions.PORT, databaseProperties.port.toInt())
        .option(ConnectionFactoryOptions.DATABASE, databaseProperties.database)
        .option(ConnectionFactoryOptions.USER, databaseProperties.username)
        .option(ConnectionFactoryOptions.PASSWORD, databaseProperties.password)
        .build(),
    )

  override fun r2dbcCustomConversions(): R2dbcCustomConversions =
    R2dbcCustomConversions(storeConversions, customConverters)

  @Bean
  fun initializer(): ConnectionFactoryInitializer =
    ConnectionFactoryInitializer()
      .apply {
        setConnectionFactory(connectionFactory())
      }
}
