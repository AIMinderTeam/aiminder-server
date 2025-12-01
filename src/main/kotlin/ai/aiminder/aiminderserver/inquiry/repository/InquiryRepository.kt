package ai.aiminder.aiminderserver.inquiry.repository

import ai.aiminder.aiminderserver.inquiry.entity.InquiryEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InquiryRepository : CoroutineCrudRepository<InquiryEntity, UUID>
