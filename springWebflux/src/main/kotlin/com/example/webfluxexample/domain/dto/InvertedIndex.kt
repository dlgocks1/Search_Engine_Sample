package com.example.webfluxexample.domain.dto

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "invertedIndex")
data class InvertedIndex(
    @Id
    val term: String,
    val postings: List<String>
)

