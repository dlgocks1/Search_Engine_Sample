package com.navercorp.searchenginesample

import com.navercorp.searchenginesample.domain.HatenaEntry
import com.navercorp.searchenginesample.repository.InvertedIndexRepository
import org.apache.lucene.analysis.ngram.NGramTokenizer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*

@SpringBootTest
class SearchenginesampleApplicationTests {

    @Autowired
    private lateinit var invertedIndexRepository: InvertedIndexRepository

    @Test
    fun nGramTokenizerTest() {
        val text = "Elasticsearch"
        val ngramTokens = performNgramTokenization(text, 2, 4)
        ngramTokens.forEach { println(it) }
    }


    fun performNgramTokenization(text: String, minGram: Int, maxGram: Int): List<String> {
        val tokenList = mutableListOf<String>()

        NGramTokenizer(minGram, maxGram).use { tokenizer ->
            tokenizer.setReader(StringReader(text))
            val charTermAttr: CharTermAttribute = tokenizer.addAttribute(CharTermAttribute::class.java)
            tokenizer.reset()
            while (tokenizer.incrementToken()) {
                val token = charTermAttr.toString()
                tokenList.add(token)
            }
            tokenizer.end()
            tokenizer.close()
        }

        return tokenList
    }

    @Test
    fun fileReadTest() {
        val filePath = "src/main/resources/10entries.txt"
        try {
            val fileContent = readTextFile(filePath)
            println(fileContent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readTextFile(filePath: String): List<HatenaEntry> {
        val file = File(filePath)
        val result = mutableListOf<HatenaEntry>()
        try {
            val bufferedReader = BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_16))
            var line: String? = bufferedReader.readLine()

            while (line != null) {
                val splited = line.split("\t")
                if (splited.isNotEmpty()) {
                    result.add(
                        HatenaEntry(
                            eid = splited[0],
                            categoryId = splited.getOrNull(1) ?: "",
                            url = splited.getOrNull(2) ?: "",
                            title = splited.getOrNull(3) ?: ""
                        )
                    )
                }
                line = bufferedReader.readLine()
            }
            bufferedReader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun makeInvetedIndex(): Map<String, PriorityQueue<String>> {
        val map: MutableMap<String, PriorityQueue<String>> = mutableMapOf()
        val filePath = "src/main/resources/10entries.txt"
        val fileContent = readTextFile(filePath)
        fileContent.forEach {
            val ngramTokens = performNgramTokenization(it.title, 2, 3)
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

    @Test
    fun query() {
        val keyword = "動画まとめブログ"
        val invertedIndex = makeInvetedIndex()
        val eidSet = mutableSetOf<String>()
        performNgramTokenization(keyword, 2, 3).forEach { token ->
            eidSet.addAll(invertedIndex[token] ?: emptyList())
        }

        eidSet.forEach {
            println(getContent(it))
        }
    }

    fun getContent(filePath: String): StringBuilder {
        val file = File("src/main/resources/texts/${filePath}")
        val result = StringBuilder()
        try {
            val bufferedReader = BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_16LE))
            result.append(bufferedReader.readLines().take(10).joinToString { "${it}\n" })
            bufferedReader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
