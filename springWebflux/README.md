# WebFlux를 통해 구현해보기

기존의 MVC환경의 구현체를 WebFlux로 교체하고, 성능을 테스트합니다.

## WebFlux란??

`Spring MVC`의 경우는 하나의 요청에 대해 하나의 쓰레드가 사용됩니다. (`thread-per-request`) 따라서 미리 스레드 풀을 생성해 놓으며, 각 요청마다 스레드를 할당하여 처리합니다.

`WebFlux`같은 경우 논블로킹과 고정된 스레드 수 만으로 모든 요청을 처리함으로 `Spring MVC`의 문제들을 해결합니다.

`WebFlux`의 서버는 하나의 스레드로 운영되며, 디폴트로 `CPU` 코어 수 개수의 스레드를 가진 워커 풀을 생성하여 해당 풀 내의 스레드로 모든 요청을 처리합니다. 제약이 있다면 논 블로킹으로 동작해야하며,
블로킹 라이브러리가 사용되어야 한다면 워커쓰레드가 아닌 외부 별도의 쓰레드로 이를 처리해야합니다. (`Event Loop`가 절대 블로킹 되지 않아야 하기 때문에)

## 성능 테스트

#### 테스트 환경은 다음과 같습니다.

- 총 아이템 `1만개`
  ![img.png](img.png)

- 인덱스 테이블 `360만개`
  ![img_1.png](img_1.png)

#### 테스트 코드는 다음과 같습니다.

```kotlin
@Test
@DisplayName("N번 만큼 API를 호출했을 때 평균 요청 시간을 구한다.")
fun apiCallTest() = runTest {
        val webClient: WebClient =
            WebClient.builder().baseUrl("http://localhost:8080").build() // Spring Boot에 의해 자동 주입됨
        val numRequests = 1000

        val apiCalls = (0..numRequests).map {
            async {
                measureTimeMillis {
                    // API 호출
                    webClient
                        .get()
                        .uri("/hatena/query?query=\"twitter\"")
                        .retrieve()
                        .bodyToMono(String::class.java)
                        .awaitFirstOrNull()
                }
            }
        }

        println("Total requests: $numRequests")
        println("Average request time: ${apiCalls.awaitAll().average()} ms")
    }
```

1000번 같은 `URL`을 요청했을 때 각각의 평균 시간을 측정하여 보겠습니다.

### Spring MVC

기본 Spring Boot 세팅

- max thread pool : 200
- min-spare thread pool : 10
- max-connections : 8192
- accept-count : 100

```text
Total requests: 1000
Average request time: 1396.2907092907094 ms
```

Spring MVC의 경우 1000번의 요청이 동시에 들어올 시 요청 당 약 `1.3`초가 소모됨을 알 수 있습니다.

### Spring Webflux

```text
Total requests: 1000
AveraAverage request time: 1540.994005994006 ms
```

Spring WebFlux의 경우 1000변의 요청이 들어올 시 요청 당 약 `1.5`초가 소모됨을 알 수 있습니다.

> 속도의 차가 유의미하지 않은 것 같기에 3000으로 늘려서 다시 실험해 봤습니다.

### Spring MVC

```text
Total requests: 3000
Average request time: 2341.47784071976 ms
```

### Spring Webflux

```text
Total requests: 3000
Average request time: 3264.9870043318892 ms
```

`Webflux`가 더 느리게 측정되는 것을 확인

`Why?` -> 이유 모르곘음..

`webFlux`내부 로직의 문제인 것 같아 수정 중