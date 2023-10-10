package com.example.webfluxexample.config

import com.example.webfluxexample.domain.common.CommonResponse
import com.example.webfluxexample.handler.CrudHandler
import com.example.webfluxexample.handler.HatenaHandler
import com.example.webfluxexample.handler.HealthHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.ConversionException
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.*

@Configuration
class RouterConfig(
    private val healthHandler: HealthHandler,
    private val crudHandler: CrudHandler,
    private val hatenaHandler: HatenaHandler
) {

    /** This route is for test */
    @Bean
    fun healthRoutes(): RouterFunction<ServerResponse> {
        return RouterFunctions.route(
            RequestPredicates.GET("/category").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
            HandlerFunction(healthHandler::checkHealth)
        )
    }

    @Bean
    fun routes(): RouterFunction<ServerResponse> = coRouter {
        accept(MediaType.APPLICATION_JSON).nest {
            exampleRoutes()
            hatenaRoutes()
        }
        handleExceptions()
    }

    private fun CoRouterFunctionDsl.hatenaRoutes() {
        "/hatena".nest {
            "categoryId".nest {
                GET("", hatenaHandler::findByCategoryId)
            }
            "query".nest {
                GET("", hatenaHandler::query)
            }
            "/{id}".nest {
                GET("", hatenaHandler::findById)
            }
        }
    }

    private fun CoRouterFunctionDsl.exampleRoutes() {
        "/example".nest {
            "/{id}".nest {
                GET("", crudHandler::findById)
                DELETE("", crudHandler::deleteById)
            }
            POST("", crudHandler::create)
        }
    }

    /** global exception handler */
    private fun CoRouterFunctionDsl.handleExceptions() {
        onError<ConversionException> { throwable: Throwable, serverRequest ->
            CommonResponse.onFailure("401", throwable.message)
        }

        onError<Exception> { throwable: Throwable, serverRequest ->
            CommonResponse.onFailure("400", throwable.message)
        }
    }
}