package com.example.webfluxexample.handler

import com.example.webfluxexample.domain.common.CommonResponse
import com.example.webfluxexample.domain.dto.SearchResult
import com.example.webfluxexample.repository.HatenaEntryRepository
import com.example.webfluxexample.repository.InvertedIndexRepository
import com.example.webfluxexample.service.HatenaSearchService
import com.example.webfluxexample.service.NGramTokenizer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import kotlin.jvm.optionals.getOrNull

@Component
class HatenaHandler(
    private val hatenaEntryRepository: HatenaEntryRepository,
    private val hatenaSearchService: HatenaSearchService,
    private val invertedIndexRepository: InvertedIndexRepository,
    private val nGramTokenizer: NGramTokenizer,
) {

    suspend fun findById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id")
        val result = hatenaEntryRepository.findById(id).asFlow().firstOrNull()
            ?: throw IllegalStateException("${id}는 존재하지 않습니다.")
        return CommonResponse.onSuccess(result)
    }

    suspend fun findByCategoryId(request: ServerRequest): ServerResponse = coroutineScope {
        val categoryId = request.queryParam("categoryId").getOrNull()
            ?: throw IllegalStateException("유효하지 않는 필드입니다.")
        return@coroutineScope CommonResponse.onSuccess(
            hatenaEntryRepository.findAllByCategoryId(categoryId).asFlow().toList()
        )
    }

    suspend fun query(request: ServerRequest): ServerResponse {
        val keyword = request.queryParam("query").orElseThrow { IllegalStateException("유효하지 않는 필드입니다.") }
        val ngramTokenization = nGramTokenizer.performNgramTokenization(
            keyword,
            HatenaSearchService.MIN_GRAM,
            HatenaSearchService.MAX_GRAM
        )

        val invertedIndex = Flux.just(ngramTokenization)
            .flatMap { invertedIndexRepository.findAllById(it) }
            .flatMap { invertedIndex -> Flux.fromIterable(invertedIndex.postings) }
            .take(10)
            .publishOn(Schedulers.boundedElastic())

        val result: Flux<SearchResult> = invertedIndex
            .flatMapSequential { eid ->
                hatenaEntryRepository.findById(eid).map {
                    SearchResult(
                        eid = it._id,
                        title = it.title,
                        url = it.url
                    )
                }
            }
        return ServerResponse.ok().bodyValue(result.collectList().awaitSingle()).awaitSingle()
    }

}