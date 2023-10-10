package com.navercorp.searchenginesample.controller

import com.navercorp.searchenginesample.domain.SearchResult
import com.navercorp.searchenginesample.service.SearchService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class SearchController(
    private val searchService: SearchService
) {

    @GetMapping("/hatena/query")
    fun query(
        @RequestParam("query") query: String
    ): List<SearchResult> {
        return searchService.query(query)
    }

    @GetMapping("/health")
    fun healthCheck(): String {
        return "I'm Alive"
    }
}