package com.navercorp.searchenginesample.service

import org.apache.lucene.analysis.ngram.NGramTokenizer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.springframework.stereotype.Component
import java.io.StringReader

@Component
class NGramTokenizer {

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
}