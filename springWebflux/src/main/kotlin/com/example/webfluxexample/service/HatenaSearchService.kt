package com.example.webfluxexample.service

import com.example.webfluxexample.domain.dto.SearchResult
import com.example.webfluxexample.repository.HatenaEntryRepository
import com.example.webfluxexample.repository.InvertedIndexRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Flux.fromIterable
import reactor.core.scheduler.Schedulers

@Service
class HatenaSearchService(
    private val invertedIndexRepository: InvertedIndexRepository,
    private val hatenaEntryRepository: HatenaEntryRepository,
    private val nGramTokenizer: NGramTokenizer,
    private val contentReader: ContentReader
) {

    private val logger = LoggerFactory.getLogger(HatenaSearchService::class.java)

    suspend fun query(keyword: String): Flux<SearchResult> {
        val ngramTokenization = nGramTokenizer.performNgramTokenization(keyword, MIN_GRAM, MAX_GRAM)
        val invertedIndex = ngramTokenization
            .let { invertedIndexRepository.findAllById(it) }
            .flatMap { invertedIndex -> fromIterable(invertedIndex.postings) }
            .take(10)
            .publishOn(Schedulers.boundedElastic())

        return invertedIndex
            .flatMapSequential { eid ->
                hatenaEntryRepository.findById(eid).map {
                    SearchResult(
                        eid = it._id,
                        title = it.title,
                        url = it.url
                    )
                }
//            .collectList()
//            .awaitSingle()
            }
    }

    companion object {
        const val MIN_GRAM = 2
        const val MAX_GRAM = 3
    }
}
