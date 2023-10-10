package com.example.webfluxexample.service

import com.example.webfluxexample.domain.dto.HatenaEntry
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@Component
class ContentReader {

    fun getContent(filePath: String): String? {
        val file = File("src/main/resources/texts/${filePath}")
        val result = StringBuilder()
        try {
            val bufferedReader = BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_16LE))
            result.append(bufferedReader.readLines().take(3).joinToString { "${it}\n" })
            bufferedReader.close()
        } catch (e: Exception) {
//            e.printStackTrace()
            return null
        }
        return result.toString()
    }

    fun readMetaFile(filePath: String): List<HatenaEntry> {
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
                            _id = splited[0],
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
}