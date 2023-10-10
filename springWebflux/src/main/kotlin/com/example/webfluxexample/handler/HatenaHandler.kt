package com.example.webfluxexample.handler

import com.example.webfluxexample.domain.common.CommonResponse
import com.example.webfluxexample.repository.HatenaEntryRepository
import com.example.webfluxexample.service.HatenaSearchService
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import kotlin.jvm.optionals.getOrNull

@Component
class HatenaHandler(
    private val hatenaEntryRepository: HatenaEntryRepository,
    private val hatenaSearchService: HatenaSearchService,
) {

    suspend fun findById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id")
        val result = hatenaEntryRepository.findById(id).asFlow().firstOrNull()
            ?: throw IllegalStateException("${id}는 존재하지 않습니다.")
        return CommonResponse.onSuccess(result)
    }

    suspend fun findByCategoryId(request: ServerRequest): ServerResponse {
        val categoryId = request.queryParam("categoryId").getOrNull()
            ?: throw IllegalStateException("유효하지 않는 필드입니다.")
        return CommonResponse.onSuccess(
            hatenaEntryRepository.findAllByCategoryId(categoryId).asFlow().toList()
        )
    }

    suspend fun query(request: ServerRequest): ServerResponse {
        val query = request.queryParam("query").getOrNull()
            ?: throw IllegalStateException("유효하지 않는 필드입니다.")
        return CommonResponse.onSuccess(
            hatenaSearchService.query(query)
        )
    }
}