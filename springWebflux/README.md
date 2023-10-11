> Webflux 및 MVC의 비교 및 Webflux의 올바른 사용방법

`Webflux` 어플리케이션은 일반적인 `Web MVC` 보다 처리량 및 응답시간 측면에서 유리하다고 알려져 있습니다. 하지만 잘못된 방법으로 `Webflux`를 사용하게 되면 `Web MVC`를 사용하는 것만
못하게 됩니다.

광고서버는 빠른 응답속도와 처리량이 필요합니다. 따라서 `Webflux`를 사용하는 것이 적합한 환경이며 `Webflux`를 활용하기 위해 `Reactive API`를 제공하는 `Mongo` 및 `Redis`가 주로
사용됩니다.

이에 따라 `Webflux`를 활용해보고, 성능 측정과 성능 개선을 한 경험을 공유하고자 합니다.

### Webflux를 활용하는 이유

`Spring MVC`는 스레드 `per Request Model`로 구현되어 있습니다. 즉, 클라이언트가 요청을 날리면 서버는 `스레드풀`에 있는 쓰레드 하나를 뽑아서 할당합니다. 할당된 쓰레드는 요청을 받고
응답할 때 까지 모든 처리를 담당합니다.

- 스프링부트의 자동 설정은 최대 200개 까지 설정되어 있습니다.

![](./image/Pasted%20image%2020231011132010.png)

위의 그림과 같이 하나의 쓰레드에서 `Database`에 접근하거나, 다른 서버의 `API`요청을 시도할 때 응답이 올 때 까지 `Block`되게 됩니다. 만약 우측의 서버가 데이터를 처리하는 시간이 길어지면, 좌측
서버 또한 응답시간이 길어지게 됩니다.

이런 작업은 `Context Swiching`을 발생시키며 요청이 다중으로 겹쳐 200개의 쓰레드가 발생하게 된다면 `CPU`에 대한 부담이 커지게 됩니다.

#### Webflux의 구조

> `Webflux`는 모두 **이벤트루프 모델**로 동작합니다.

![](./image/Pasted%20image%2020231011132650.png)

사용자들의 요청이나 어플리케이션 내부 작업은 `Event`로 관리되고 `Event Queue`에 적재되어 처리됩니다. 이런 `Event Queue`를 처리하는 쓰레드 풀이 따로 존재합니다.

- 이 쓰레드 풀은 순차적으로 이벤트를 처리한다고 하여 `Event Loop`라고 하기도 합니다.

`Spring Webflux`는 리액터 라이브러리와 `netty`를 기반으로 동작합니다. (`Tomcat`과 다르게 `netty`의 경우 `NonBlocking I/O`를 제공합니다) 이에 따라서 쓰레드풀의 쓰레드
개수는 `CPU Core`의 수 \* 2 로 지정됩니다.

![](./image/Pasted%20image%2020231011132951.png)

`REST API 호출`, `I/O의 결과 처리` 모두 이벤트로 처리되어 이벤트 루프에 의해 처리됩니다. 따라서 `Context Switching`오버헤드가 줄어들고, `Non Blocking`이기에 처리량이
증가합니다.

---

**하지만**

`Webflux`는 `CPU`사용량이 많은 작업을 다수 실행하거나, `blocking I/O`를 이용하여 프로그래밍 한다면 적은 쓰레드의 특성상 성능이 저하될 수 밖에 없습니다.

- ex) 동영상 인코딩, 문서 암호화 및 복호화 등

## 테스트

`Spring Mvc, Webflux` 동일 로직 작성 후 `API Call Test`를 진행합니다.

> 실제 물리머신을 사용하지 않고, 로컬 환경에서의 테스트를 진행함으로 병목이 있을 수 있습니다.

#### JVM 환경

```
-Xmx4096m
-XX:+UseG1GC
-XX:NewRatio=1
-XX:MaxGCPauseMillis=100
-XX:GCTimeRatio=24
-XX:ParallelGCThreads=8
-XX:ConcGCThreads=2
```

- **-XX:+UseG1GC**:
    - G1(Garbage First) GC를 사용한다는 것을 의미합니다.
- **\*-XX:NewRatio=1**:
    - Young 영역과 Old 영역의 비율을 나타냅니다.
- **\*-XX:MaxGCPauseMillis=100**:
    - GC가 발생하는 최대 일시 정지 시간을 밀리초(ms) 단위로 제한합니다.
- **\*-XX:GCTimeRatio=24**:
    - GC 시간 비율을 나타냅니다. GC 시간과 애플리케이션 실행 시간의 비율을 조절할 수 있습니다. 이 값은 GC 시간이 (1 / (1 + GCTimeRatio))의 비율로 애플리케이션 실행 시간에 대해
      허용됩니다.
- **\*-XX:ParallelGCThreads=8**:
    - 병렬 GC의 스레드 수를 설정합니다. 이 값은 GC 작업을 병렬로 처리할 때 사용할 스레드 수를 나타냅니다.
- **\*-XX:ConcGCThreads=2**:
    - Concurrent GC(병행 GC)의 스레드 수를 설정합니다.

#### 테스트 시나리오

- `?query=amazon`
- `?query=twitter`

위의 두 `API`를 10초동안 동시에 3000번 요청 진행

#### Spring MVC

```kotlin
fun query(keyword: String): List<SearchResult> {
    val searchNgram = nGramTokenizer.performNgramTokenization(keyword, MIN_GRAM, MAX_GRAM)
    val invertedIndexes = invertedIndexRepository.findAllById(searchNgram)
        .map { it.postings }
        .flatten()
        .take(10) // 게시물 10개만 추출

    return invertedIndexes.map { eid ->
        val hatenaEntry = hatenaEntryRepository.findByIdOrNull(eid)
        SearchResult(
            eid = eid,
            title = hatenaEntry?.title ?: "",
            url = hatenaEntry?.url ?: "",
        )
    }
}
```

내부 로직은 다음과 같이 진행됩니다.

1. `Query`문을 2~3크기로 `nGramTokenizer`를 진행합니다.

- ex) twitter -> tw, twi, wi, wit, it, itt ....

2. 해당 `토큰`을 기반으로 역인덱스 테이블을 활용해 인덱스 불러옵니다.
3. 불러온 인덱스를 기반으로 10개를 추출해 글을 불러옵니다.

테스트 결과는 다음과 같습니다.

| Label         | Sample Count | AVERAGE | 90% | 95% | 99% | MIN | MAX | Throughput |
|---------------|--------------|---------|-----|-----|-----|-----|-----|------------|
| Query Amazon  | 3000         | 23      | 40  | 187 | 319 | 3   | 423 | 333.1      |
| Query Twitter | 3000         | 18      | 21  | 147 | 213 | 4   | 286 | 348.3      |
| TOTAL         | 6000         | 40      | 199 | 256 | 353 | 3   | 581 | 665.8      |

> 평균 응답시간 : 40

### Spring Webflux

#### Test#1

기존의 코드를 `Coroutine` 및 `Flow`를 활용해 구현

```kotlin
suspend fun query(keyword: String): List<SearchResult>? = coroutineScope {
    val searchNgram = nGramTokenizer.performNgramTokenization(keyword, MIN_GRAM, MAX_GRAM)
    invertedIndexRepository.findAllById(searchNgram)
        .asFlow()
        .toList()
        .flatMap { invertedIndex -> invertedIndex.postings }
        .take(10)
        .map { eid ->
            async(Dispatchers.IO) {
                val hatenaEntry = hatenaEntryRepository.findById(eid).asFlow().firstOrNull()
                SearchResult(
                    eid = eid,
                    title = hatenaEntry?.title ?: "-",
                    url = hatenaEntry?.url ?: "-",
                )
            }
        }.awaitAll()
}
```

| Label         | Sample Count | AVERAGE | 90%  | 95%  | 99%  | MIN | MAX  | Throughput |
|---------------|--------------|---------|------|------|------|-----|------|------------|
| Query Amazon  | 3000         | 1054    | 1484 | 1537 | 1608 | 367 | 1707 | 273.0      |
| Query Twitter | 3000         | 1245    | 1714 | 1773 | 1822 | 502 | 1910 | 284.3      |
| TOTAL         | 6000         | 1149    | 1593 | 1714 | 1808 | 367 | 1910 | 539.5      |

뭔가가 이상합니다.

> 평균 응답시간 `40 -> 1149`

위의코드가 느린 이유로는

- WebFlux는 Reactor 라이브러리를 기반으로 하며, `Scheduler`객체를 통해 쓰레드를 효율적으로 관리하여 비동기 작업을 처리합니다.
- 이에 반해 Flow는 Kotlin의 표준 라이브러리이며, 별도의 `Scheduler`를 사용하지 않으며, `webFlux`와의 최적화 수준이 떨어질 수 있습니다.

따라서 `Mono` 및 `Flux`를 활용하는 코드로 바꿔보았습니다.

#### Test#2

```kotlin
suspend fun query(keyword: String): List<SearchResult> = coroutineScope {
    val searchNgram = nGramTokenizer.performNgramTokenization(keyword, MIN_GRAM, MAX_GRAM)
    invertedIndexRepository.findAllById(searchNgram)
        .collectList() // Flux 활용하도록 변경
        .awaitSingle()
        .flatMap { it.postings }
        .take(10)
        .map { eid ->
            async(Dispatchers.IO) {
                val hatenaEntry = hatenaEntryRepository.findById(eid).awaitSingle() // Flux 활용하도록 변경
                SearchResult(
                    eid = eid,
                    title = hatenaEntry?.title ?: "-",
                    url = hatenaEntry?.url ?: "-",
                )
            }
        }.awaitAll()
}
```

| Label         | Sample Count | AVERAGE | 90%  | 95%  | 99%  | MIN | MAX  | Throughput |
|---------------|--------------|---------|------|------|------|-----|------|------------|
| Query Amazon  | 3000         | 1727    | 2141 | 2175 | 2222 | 419 | 2255 | 275.6      |
| Query Twitter | 3000         | 1983    | 2484 | 2511 | 2548 | 864 | 2600 | 268.6      |
| TOTAL         | 6000         | 1855    | 2399 | 2499 | 2484 | 419 | 2600 | 507.5      |

뭔가가 더 이상합니다.

> 평균 응답시간 `40 -> 1855`

기존의 `Flow`를 간단히 `flux`, `mono`로 바꾸는 것 만으로는 해결되지 않는 것 같습니다.

그 이유는 위와 같이 `awaiteSingle`을 사용하는 것 조차도 동기식으로 진행되기 때문에 성능에 저하가 오게 됩니다.

> `Webflux`는 `CPU`사용량이 많은 작업을 다수 실행하거나, `blocking I/O`를 이용하여 프로그래밍 한다면 적은 쓰레드의 특성상 성능이 저하될 수 밖에 없습니다.

따라서 데이터 스트림을 최대한 활용하는 형태로 다시 코드를 수정하였습니다.

#### Test#3

```kotlin
suspend fun query(keyword: String): List<SearchResult> = coroutineScope {

    val ngramTokenization = async {
        nGramTokenizer.performNgramTokenization(keyword, MIN_GRAM, MAX_GRAM)
    }
    val invertedIndex = ngramTokenization.await()
        .let { invertedIndexRepository.findAllById(it) }
        .flatMap { invertedIndex -> fromIterable(invertedIndex.postings) }
        .take(10)

    val searchResults = invertedIndex
        .map { eid ->
            hatenaEntryRepository.findById(eid).let {
                SearchResult(
                    eid = it._id,
                    title = it.title,
                    url = it.url
                )
            }
        }
        .collectList()
        .awaitSingle() // 결과를 도출해내기 위해서 결국 마지막에는 Await를 해야함

    searchResults
}
```

| Label         | Sample Count | AVERAGE | 90% | 95% | 99% | MIN | MAX | Throughput |
|---------------|--------------|---------|-----|-----|-----|-----|-----|------------|
| Query Amazon  | 3000         | 64      | 300 | 336 | 407 | 1   | 463 | 332.2      |
| Query Twitter | 3000         | 55      | 242 | 308 | 339 | 3   | 379 | 351.0      |
| TOTAL         | 6000         | 59      | 284 | 322 | 373 | 1   | 463 | 666.2      |

드디어 결과 값이 비교해볼 수 있을만큼 좋아졌습니다.

| Label   | Sample Count | AVERAGE | 90% | 95% | 99% | MIN | MAX | Throughput |
|---------|--------------|---------|-----|-----|-----|-----|-----|------------|
| Webflux | 6000         | 59      | 284 | 322 | 373 | 1   | 463 | 666.2      |
| MVC     | 6000         | 40      | 199 | 256 | 353 | 3   | 581 | 665.8      |

하지만 그럼에도 불구하고 `MVC`보다는 여전히 느린 속도를 보여줍니다.
특히 상위 `90%`의 결과값의 경우는 `284ms`와 `199ms`로 1.5배 정도 차이가 납니다.

1. **이를 개선하기 위해서 `Scheduler`를 활용할 수 있습니다.**

> `Flux`와 `Mono`는 기본적으로 메인쓰레드 하나에서 실행됩니다. 따라 `Scheduler`를 활용해 `next`, `complete`, `error`신호를 별도 쓰레드로 처리할 수 있습니다.

`map, filter, flatMap`변환 등과 같은 연산은 `publishOn()`을 활용해 별도의 쓰레드에서 실행할 수 있습니다.

2. **`map`과 `flatmap`의 차이를 알고 활용해야 합니다.**

리액터 `Javadoc`에서는 다음과 같이 설명하고 있습니다.

- **map ()** - Transform the item emitted by this Mono by applying a synchronous function to it
- **flatMap()** - Transform the item emitted by this Mono asynchronously, returning the value emitted by another Mono

> `map()`은 동기식 함수를 적용하여 `Mono`의 아이템을 변경하는데 사용되는 메서드입니다.
>
> `flatMap()`은 `Async`하게 `Mono`의 아이템을 변경하는데 사용되는 메서드입니다.

따라서 비동기 처리 부분은 `flatmap`으로, 동기처리 부분은 `map`함수를 사용해야 합니다.

#### Test#4

```kotlin
suspend fun query(keyword: String): List<SearchResult>? = coroutineScope {

    val ngramTokenization = async {
        nGramTokenizer.performNgramTokenization(keyword, MIN_GRAM, MAX_GRAM)
    }
    val invertedIndex = ngramTokenization.await()
        .let { invertedIndexRepository.findAllById(it) }
        .flatMap { invertedIndex -> fromIterable(invertedIndex.postings) }
        .take(10)
        .publishOn(Schedulers.boundedElastic()) // 이후의 작업은 모두 별도의 쓰레드에서 실행할 것을 의미

    val searchResults = invertedIndex
        .flatMapSequential { eid ->
            hatenaEntryRepository.findById(eid)
                .map {
                    SearchResult(
                        eid = it._id,
                        title = it.title,
                        url = it.url
                    )
                }
        }
        .collectList()
        .awaitSingle()

    searchResults
}
```

`webFlux`에서는 일반적인 스케줄러로 `Scheduler.boundedElastic()`를 제공합니다.

- 기본적으로 쓰레드의 수는 `CPU`코어 수의 10배, `100_000`개의 `queue cap`이 생성됩니다.
- 해당 쓰레드는 워커 생성시에 한번만 수행되고 이후 캐시되어 사용됩니다.
    - 따라서 두 워커가 동시에 사용시 지연될 수 있습니다.

또한 순서를 보장하는 `flatMapSequential`를 활용하여 비동기 처리를 구현합니다.

| Label         | Sample Count | AVERAGE | 90% | 95% | 99% | MIN | MAX | Throughput |
|---------------|--------------|---------|-----|-----|-----|-----|-----|------------|
| Query Amazon  | 3000         | 55      | 273 | 322 | 401 | 1   | 481 | 332.2      |
| Query Twitter | 3000         | 46      | 211 | 377 | 338 | 2   | 367 | 351.5      |
| TOTAL         | 6000         | 51      | 253 | 302 | 366 | 1   | 481 | 666.1      |

| Label   | Sample Count | AVERAGE | 90% | 95% | 99% | MIN | MAX | Throughput |
|---------|--------------|---------|-----|-----|-----|-----|-----|------------|
| WEBFLUX | 6000         | 41      | 238 | 274 | 419 | 1   | 481 | 666.1      |
| MVC     | 6000         | 40      | 199 | 256 | 353 | 3   | 581 | 665.8      |

스케줄러로 비동기 작업을 별도의 쓰레드로 넘김으로써 `MVC`와 엇비슷한 응답시간을 낼 수 있습니다.

### CPU 사용량 비교

성능적인 측면을 비교해보겠습니다.

- Webflux
  ![](./image/Pasted%20image%2020231011164503.png)

- MVC
  ![](./image/Pasted%20image%2020231011164648.png)

차이가 거의 없음을 확인할 수 있습니다.

## 참고자료

[[NHN FORWARD 2020] 내가 만든 WebFlux가 느렸던 이유](https://www.youtube.com/watch?v=I0zMm6wIbRI)
[Spring.io - Webflux](https://docs.spring.io/spring-framework/reference/web/webflux.html)
