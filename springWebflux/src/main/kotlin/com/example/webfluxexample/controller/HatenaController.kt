package com.example.webfluxexample.controller

import com.example.webfluxexample.domain.dto.SearchResult
import com.example.webfluxexample.repository.HatenaEntryRepository
import com.example.webfluxexample.repository.InvertedIndexRepository
import com.example.webfluxexample.service.HatenaSearchService
import com.example.webfluxexample.service.NGramTokenizer
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux


@RestController
class HatenaController(
    private val hatenaEntryRepository: HatenaEntryRepository,
    private val invertedIndexRepository: InvertedIndexRepository,
    private val nGramTokenizer: NGramTokenizer,
) {

    @GetMapping("hatena/query")
    fun query(@RequestParam("query") keyword: String): Flux<SearchResult> {
        return Flux.just(
            nGramTokenizer.performNgramTokenization(
                keyword,
                HatenaSearchService.MIN_GRAM,
                HatenaSearchService.MAX_GRAM
            )
        ).flatMap { invertedIndexRepository.findAllById(it) }
            .flatMapIterable { it.postings }
            .take(400)
            .flatMap {
                hatenaEntryRepository.findById(it).map {
                    SearchResult(
                        eid = it._id,
                        title = it.title,
                        url = it.url
                    )
                }
            }
    }
}