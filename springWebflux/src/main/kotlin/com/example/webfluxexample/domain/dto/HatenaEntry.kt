package com.example.webfluxexample.domain.dto

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("hatenaEntry")
data class HatenaEntry(
    @Id
    val _id: String,
    val categoryId: String,
    val title: String,
    val url: String
)


