package com.example.webfluxexample.handler

import com.example.webfluxexample.domain.common.CommonResponse
import com.example.webfluxexample.domain.dto.ExampleItem
import com.example.webfluxexample.repository.ExampleItemRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.asFlow
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody

@Component
class CrudHandler(
    private val exampleItemRepository: ExampleItemRepository
) {

    suspend fun create(request: ServerRequest): ServerResponse {
        val exampleDto = request.awaitBody<ExampleItem>()
        val result =
            exampleItemRepository.save(exampleDto).asFlow().firstOrNull()
                ?: throw IllegalStateException("데이터 저장에 실패하였습니다.")
        return CommonResponse.onSuccess(result)
    }

    suspend fun findById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id")
        val result = exampleItemRepository.findById(ObjectId(id)).asFlow().firstOrNull()
            ?: throw IllegalStateException("$id 는 존재하지 않습니다.")
        return CommonResponse.onSuccess(result)
    }

    suspend fun deleteById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id")
        exampleItemRepository.deleteById(ObjectId(id)).asFlow().firstOrNull()
        return CommonResponse.onSuccess("값 삭제 성공")
    }
}