package com.navercorp.searchenginesample.repository

import com.navercorp.searchenginesample.domain.HatenaEntry
import org.springframework.data.mongodb.repository.MongoRepository

interface HatenaEntryRepository : MongoRepository<HatenaEntry, String>
