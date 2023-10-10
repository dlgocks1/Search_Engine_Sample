package com.example.webfluxexample.handler

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class HealthHandler {

    fun checkHealth(req: ServerRequest): Mono<ServerResponse> {
        return ServerResponse.ok().body(Mono.just("I'm healthy"), String::class.java)
    }
}
