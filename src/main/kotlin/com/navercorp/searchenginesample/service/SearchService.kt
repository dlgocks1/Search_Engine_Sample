package com.navercorp.searchenginesample.service

import com.navercorp.searchenginesample.domain.SearchResult
import com.navercorp.searchenginesample.repository.HatenaEntryRepository
import com.navercorp.searchenginesample.repository.InvertedIndexRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class SearchService(
    private val invertedIndexRepository: InvertedIndexRepository,
    private val hatenaEntryRepository: HatenaEntryRepository,
    private val nGramTokenizer: NGramTokenizer,
    private val contentReader: ContentReader
) {

    fun query(keyword: String): List<SearchResult> {
        val searchNgram = nGramTokenizer.performNgramTokenization(keyword, MIN_GRAM, MAX_GRAM)
        val invertedIndexes = invertedIndexRepository.findAllById(searchNgram)
        return invertedIndexes.mapNotNull { invertedIndex ->
            invertedIndex.postings.map { eid ->
                val hatenaEntry = hatenaEntryRepository.findByIdOrNull(eid)
                SearchResult(
                    title = hatenaEntry?.title ?: "",
                    url = hatenaEntry?.url ?: "",
                    snippet = contentReader.getContent(eid) ?: ""
                )
            }
        }.flatten()
    }


    companion object {
        const val MIN_GRAM = 2
        const val MAX_GRAM = 3
    }
}