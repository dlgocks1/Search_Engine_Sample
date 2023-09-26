package com.navercorp.searchenginesample.service

import com.navercorp.searchenginesample.domain.InvertedIndex
import com.navercorp.searchenginesample.repository.HatenaEntryRepository
import com.navercorp.searchenginesample.repository.InvertedIndexRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.util.*


@Component
class DataInitializer(
    private val nGramTokenizer: NGramTokenizer,
    private val contentReader: ContentReader,
    private val invertedIndexRepository: InvertedIndexRepository,
    private val hatenaEntryRepository: HatenaEntryRepository
) : CommandLineRunner {

    @Throws(Exception::class)
    override fun run(vararg args: String) {
        hatenaEntryRepository.deleteAll()
        invertedIndexRepository.deleteAll()
        // 역 인덱스 저장
        invertedIndexRepository.saveAll(makeInvertedIndex().map {
            InvertedIndex(
                term = it.key,
                postings = it.value.toList()
            )
        })
        // eid를 키값으로하는 데이터 MongoDB에 저장
        hatenaEntryRepository.saveAll(contentReader.readMetaFile("src/main/resources/10000entries.txt"))
    }

    private fun makeInvertedIndex(): Map<String, PriorityQueue<String>> {
        val map: MutableMap<String, PriorityQueue<String>> = mutableMapOf()
        val filePath = "src/main/resources/10000entries.txt"
        val fileContent = contentReader.readMetaFile(filePath)
        fileContent.forEach {
            val ngramTokens = nGramTokenizer.performNgramTokenization(it.title, 2, 3)
            ngramTokens.forEach { token ->
                if (map[token] == null) {
                    map[token] = PriorityQueue<String>().apply { add(it.eid) }
                } else {
                    map[token] = map[token]!!.apply { add(it.eid) }
                }
            }
        }
        return map
    }
}