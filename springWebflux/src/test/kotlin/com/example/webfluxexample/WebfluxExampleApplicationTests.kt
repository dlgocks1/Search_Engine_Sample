package com.example.webfluxexample

import com.example.webfluxexample.service.HatenaSearchService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest
class WebfluxExampleApplicationTests {

    @Autowired
    private lateinit var hatenaSearchService: HatenaSearchService

    @Test
    fun contextLoads() = runTest {
        val time = (0..100).map {
            async {
                measureTimeMillis {
                    hatenaSearchService.query("twitter")
                }
            }
        }
        time.awaitAll().also {
            println(it.average())
        }
    }

}
