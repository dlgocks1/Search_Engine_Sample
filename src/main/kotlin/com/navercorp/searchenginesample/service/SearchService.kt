package com.navercorp.searchenginesample.service

import com.navercorp.searchenginesample.domain.SearchResult
import com.navercorp.searchenginesample.repository.HatenaEntryRepository
import com.navercorp.searchenginesample.repository.InvertedIndexRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class SearchService(
    private val invertedIndexRepository: InvertedIndexRepository,
    private val hatenaEntryRepository: HatenaEntryRepository,
    private val nGramTokenizer: NGramTokenizer,
    private val contentReader: ContentReader
) {

    private val logger = LoggerFactory.getLogger(SearchService::class.java)

    fun query(keyword: String): List<SearchResult> {
        val searchNgram = nGramTokenizer.performNgramTokenization(keyword, MIN_GRAM, MAX_GRAM)
        val invertedIndexes = invertedIndexRepository.findAllById(searchNgram)
        return invertedIndexes.mapNotNull { invertedIndex ->
            invertedIndex.postings.map { eid ->
                val hatenaEntry = hatenaEntryRepository.findByIdOrNull(eid)
                SearchResult(
                    eid = eid,
                    title = hatenaEntry?.title ?: "",
                    url = hatenaEntry?.url ?: "",
                    snippet = contentReader.getContent(eid)?.take(100) ?: ""
                )
            }
        }.flatten().sortedByDescending { it.eid }.also { logger.info("totalElements : ${it.size}") }
    }

    companion object {
        const val MIN_GRAM = 2
        const val MAX_GRAM = 3
    }
}