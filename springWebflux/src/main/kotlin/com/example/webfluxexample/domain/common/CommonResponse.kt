package com.example.webfluxexample.domain.common

import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait

data class CommonResponse<out T>(
    val isSuccess: Boolean,
    val message: String?,
    val code: String?,
    val result: T? = null,
) {

    companion object {
        suspend fun <T> onSuccess(data: T): ServerResponse {
            return ServerResponse.ok().bodyValueAndAwait(CommonResponse(true, "response is success", "1000", data))
        }

        suspend fun onFailure(code: String, message: String?): ServerResponse {
            return ServerResponse.badRequest().bodyValueAndAwait(CommonResponse<String>(false, message, code))
        }
    }
}