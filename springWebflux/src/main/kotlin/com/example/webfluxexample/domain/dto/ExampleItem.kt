package com.example.webfluxexample.domain.dto

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("example_item")
data class ExampleItem(
    @Id val _id: String,
    val name: String,
    val content: String
)
