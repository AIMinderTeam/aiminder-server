package ai.aiminder.aiminderserver.common.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import org.jooq.Publisher
import org.jooq.Record
import org.springframework.beans.factory.annotation.Autowired

open class JooqR2dbcRepository {
  @Autowired
  private lateinit var dslContext: DSLContext

  suspend fun <R : Record> query(execute: DSLContext.() -> Publisher<R>): Flow<R> =
    withContext(Dispatchers.IO) {
      execute(dslContext)
    }.asFlow()
}
