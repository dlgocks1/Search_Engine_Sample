package com.example.webfluxexample

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory
import reactor.core.publisher.BaseSubscriber
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.CountDownLatch


class FluxAndMonoTest {


    @Test
    fun fluxTest1() {
        val seq1 = Flux.just("foo", "bar", "foobar")
        val iterable = listOf("foo", "bar", "foobar")
        val seq2 = Flux.fromIterable(iterable)

        seq1.subscribe { println(it) }
        seq2.subscribe { println(it) }
    }

    @Test
    fun fluxTest2() {
        val seq1 = Flux.range(1, 4).map {
            if (it <= 3) return@map 1
            else throw RuntimeException("error on 4")
        }
        seq1.subscribe(
            { println(it) },
            { println("Error: $it") }
        )
    }

    @Test
    fun fluxTest3() {
        val seq1 = Flux.range(1, 2).map {
            if (it <= 3) return@map 1
            else throw RuntimeException("error on 4")
        }
        val test = seq1.subscribe(
            { println(it) },
            { println("Error: $it") },
            { println("onDone") },
        )
    }

    @Test
    fun fluxTest4() {
        val seq1 = Flux.range(1, 4)
        seq1.subscribe(object : BaseSubscriber<Int>() {
            override fun hookOnNext(value: Int) {
                println(value)
                request(Long.MAX_VALUE)
            }
        })
    }

    inner class SampleSubscriber<T> : BaseSubscriber<T>() {

        override fun hookOnSubscribe(subscription: Subscription) {
            println("Subscribed")
            request(1)
        }

        override fun hookOnNext(value: T & Any) {
            println(value)
            request(1)
        }
    }

    @Test
    fun fluxTest5() {
        val tick1 = Flux.interval(Duration.ofSeconds(1)).map<String> { tick: Long -> tick.toString() + "s tick" }
        val tick2 = Flux.interval(Duration.ofMillis(700)).map { tick: Long -> tick.toString() + "ms tick" }
        tick1.mergeWith(tick2).subscribe { x -> println(x) }
        runBlocking {
            delay(5000L)
        }
    }

    @Test
    @DisplayName("zipwith 테스트")
    fun fluxTest6() {
        val tick1 = Flux.interval(Duration.ofSeconds(1)).map { tick: Long -> tick.toString() + "s tick" }
        val tick2 = Flux.interval(Duration.ofMillis(700)).map { tick: Long -> tick.toString() + "ms tick" }
        tick1.zipWith(tick2).subscribe { println(it) }
        runBlocking {
            delay(5000L)
        }
    }

    @Test
    fun schedulerTest1() {
        val latch = CountDownLatch(1)
        Flux.just(1, 2, 3, 4, 5, 6)
            .map { i: Int ->
                logger.info("$i + 10")
                i + 10
            }
            .publishOn(Schedulers.boundedElastic(), 3)
            .map { i: Int ->
                logger.info("$i + 1")
                i + 1
            } //publishOn에서 지정한 스케주러가 실행
            .subscribe(object : BaseSubscriber<Int>() {
                override fun hookOnSubscribe(subscription: Subscription) {
                    logger.info("hookOnSubscribe")
                    requestUnbounded()
                }

                override fun hookOnNext(value: Int) {
                    logger.info("hookOnNext: $value") // publishOn에서 지정한 스케줄러가 실행
                }

                override fun hookOnComplete() {
                    logger.info("hookOnComplete") // publishOn에서 지정한 스케줄러가 실행
                    latch.countDown()
                }
            })
        latch.await()
    }

    @Test
    fun schedulerTest2() {
        Flux.range(1, 6)
            .publishOn(Schedulers.newBoundedElastic(1, Int.MAX_VALUE, "Pub1"))
            .map { i ->
                logger.info("map $i + 1")
                i + 1
            }
            .publishOn(Schedulers.newBoundedElastic(1, Int.MAX_VALUE, "Pub2"))
            .map { i ->
                logger.info("map $i + 10")
                i + 10
            }
            .subscribe({ println("result $it") })

        runBlocking {
            delay(3000L)
        }
    }


    @Test
    fun schedulerTest3() {
        Flux.range(1, 10)
            .parallel(2)
            .runOn(Schedulers.newParallel("Pub1", 2))
            .doOnNext { println("Value : $it on ${Thread.currentThread().name}") }
            .subscribe()

        runBlocking {
            delay(3000L)
        }
    }

    @Test
    fun collectListTest() {
        val someFlux = Flux.range(1, 10)
        val mono = someFlux.collectList()
        mono.subscribe { println(it) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FluxAndMonoTest::class.java)
    }
}