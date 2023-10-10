package com.example.webfluxexample.service

import com.example.webfluxexample.domain.dto.SearchResult
import com.example.webfluxexample.repository.HatenaEntryRepository
import com.example.webfluxexample.repository.InvertedIndexRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HatenaSearchService(
    private val invertedIndexRepository: InvertedIndexRepository,
    private val hatenaEntryRepository: HatenaEntryRepository,
    private val nGramTokenizer: NGramTokenizer,
    private val contentReader: ContentReader
) {

    private val logger = LoggerFactory.getLogger(HatenaSearchService::class.java)

    suspend fun query(keyword: String): List<SearchResult> = coroutineScope {
        val searchNgram = nGramTokenizer.performNgramTokenization(keyword, MIN_GRAM, MAX_GRAM)
        val indexes = invertedIndexRepository.findAllById(searchNgram)
            .asFlow()
            .also { logger.info("all invertedIndex finded") }
            .toList()
            .map { it.postings }
            .flatten()
            .sorted() // 스코어링 적용
            .take(10) // 게시물 10개만 추출

        val result = indexes.map { eid ->
            async {
                val hatenaEntry = hatenaEntryRepository.findById(eid).asFlow().firstOrNull()
                val snippet = async { contentReader.getContent(eid) }
                SearchResult(
                    eid = eid,
                    title = hatenaEntry?.title ?: "-",
                    url = hatenaEntry?.url ?: "-",
                    snippet = snippet.await()?.take(100) ?: "cannot load snippet"
                ).also {
                    logger.info("$eid is queried")
                }
            }
        }

        result.awaitAll()
    }

    companion object {
        const val MIN_GRAM = 2
        const val MAX_GRAM = 3
    }
}
