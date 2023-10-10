package com.example.webfluxexample.repository

import com.example.webfluxexample.domain.dto.HatenaEntry
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface HatenaEntryRepository : ReactiveMongoRepository<HatenaEntry, String> {
    fun findAllByCategoryId(categoryId: String): Flux<List<HatenaEntry>>
}