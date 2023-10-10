package com.navercorp.searchenginesample.repository

import com.navercorp.searchenginesample.domain.InvertedIndex
import org.springframework.data.mongodb.repository.MongoRepository

interface InvertedIndexRepository : MongoRepository<InvertedIndex, String>