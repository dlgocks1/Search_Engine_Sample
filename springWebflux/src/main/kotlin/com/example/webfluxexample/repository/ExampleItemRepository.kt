package com.example.webfluxexample.repository

import com.example.webfluxexample.domain.dto.ExampleItem
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface ExampleItemRepository : ReactiveMongoRepository<ExampleItem, ObjectId>