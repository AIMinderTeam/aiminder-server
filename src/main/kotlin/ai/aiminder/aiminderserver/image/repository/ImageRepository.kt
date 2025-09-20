package ai.aiminder.aiminderserver.image.repository

import ai.aiminder.aiminderserver.image.entity.ImageEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ImageRepository : CoroutineCrudRepository<ImageEntity, UUID>
