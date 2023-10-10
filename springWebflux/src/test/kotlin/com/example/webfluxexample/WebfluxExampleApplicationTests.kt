package com.example.webfluxexample

import com.example.webfluxexample.service.HatenaSearchService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest
class WebfluxExampleApplicationTests {

    @Autowired
    private lateinit var hatenaSearchService: HatenaSearchService

    @Test
    @DisplayName("N번 만큼 API를 호출했을 때 평균 요청 시간을 구한다.")
    fun apiCallTest() = runTest {

        val connectionProvider = ConnectionProvider
            .builder("myConnectionPool")
            .maxConnections(100)
            .pendingAcquireMaxCount(3000)
            .build()
        val clientHttpConnector = ReactorClientHttpConnector(HttpClient.create(connectionProvider))

        val webClient: WebClient =
            WebClient.builder()
                .clientConnector(clientHttpConnector)
                .baseUrl("http://localhost:8080").build() // Spring Boot에 의해 자동 주입됨
        val numRequests = 3000

        val apiCalls = (0..numRequests).map {
            async {
                measureTimeMillis {
                    // API 호출
                    webClient
                        .get()
                        .uri("/hatena/query?query=\"twitter\"")
                        .retrieve()
                        .bodyToMono(String::class.java)
                        .awaitFirstOrNull()
                }
            }
        }

        println("Total requests: $numRequests")
        println("Average request time: ${apiCalls.awaitAll().average()} ms")
    }

}
