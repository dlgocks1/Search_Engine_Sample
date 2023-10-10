package com.navercorp.searchenginesample.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class HatenaEntry(
    @Id
    val eid: String,
    val categoryId: String,
    val url: String,
    val title: String
)
