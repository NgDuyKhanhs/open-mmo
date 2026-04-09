package com.openmmo.ai.repository

import com.openmmo.ai.entity.CorrespondentMemory
import org.springframework.data.mongodb.repository.MongoRepository

interface CorrespondentMemoryRepository : MongoRepository<CorrespondentMemory, String> {
    fun findByUserIdAndCorrespondentEmail(userId: String, correspondentEmail: String): CorrespondentMemory?

    fun findByUserId(userId: String): List<CorrespondentMemory>
}

