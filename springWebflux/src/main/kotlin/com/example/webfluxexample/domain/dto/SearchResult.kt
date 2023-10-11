package com.example.webfluxexample.domain.dto

data class SearchResult(
    val eid: String,
    val title: String,
    val url: String,
    val snippet: String = ""
)
