package com.example.webfluxexample.repository

import com.example.webfluxexample.domain.dto.InvertedIndex
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface InvertedIndexRepository : ReactiveMongoRepository<InvertedIndex, String>