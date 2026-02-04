![header](https://capsule-render.vercel.app/api?type=wave&color=auto&height=300&section=header&text=order%20spring%20batch&fontSize=90)

# order-spring-batch

이 문서는 `order-spring-batch` 프로젝트의 개요, 실행 방법, 구성 요소 설명(특히 Spring Batch의 Job/Step/ItemReader/ItemProcessor/ItemWriter 흐름)과 확장/문제해결 팁을 초보자 관점에서 자세히 설명합니다.

---

## 개요

`order-spring-batch`는 Spring Boot + Spring Batch 기반의 예제 프로젝트입니다. CSV 파일로부터 주문 데이터를 읽어(Reader), 간단한 유효성 검사와 변환을 수행한 뒤(Processor), 성공한 레코드는 DB에 저장하는(Writer) 배치 처리를 보여줍니다. 학습용으로 설계되어 있어 실제 프로덕션 패턴(데이터베이스, 트랜잭션, 청크 처리, 리스너 등)을 간단히 체험할 수 있습니다.

주요 동작 요약(한 줄): 입력 CSV -> ItemReader -> ItemProcessor -> ItemWriter(성공: DB) -> Job/Step 종료

## 핵심 개념(초보자용)

- Job: 배치 작업의 단위(배치 애플리케이션에서 실행되는 한 묶음 작업). 여러 Step을 가질 수 있습니다.
- Step: Job을 구성하는 하위 단위. 보통 청크 기반(Chunk-oriented) 또는 태스크릿(Tasklet) 방식으로 동작합니다.
- Chunk: Step이 한 번에 처리하는 레코드 묶음 크기(예: 10개씩 읽고 변환한 뒤 쓰기). 트랜잭션 단위가 됩니다.
- ItemReader: 입력(예: CSV, DB, 큐)을 읽어 도메인/DTO 객체로 반환합니다.
- ItemProcessor: ItemReader가 반환한 객체를 검증/변환하여 ItemWriter가 저장할 형태로 만듭니다.
- ItemWriter: 처리된 데이터를 최종 저장(예: DB에 insert, 파일에 write)합니다.
- Listener: Job/Step의 시작, 종료, 예외 시 후속 작업(로깅, 통계 등)을 수행합니다.

## 전체 흐름 및 코드 매핑(핵심 클래스)

아래는 Spring Batch 개념과 이 프로젝트의 실제 파일 간 매핑입니다(파일 경로는 `src/main/java` 기준).

- Job
  - `src/main/java/com/example/batch/config/BatchConfig.java` — 메인 `Job`(예: `processOrderJob()`)
- Step
  - `src/main/java/com/example/batch/config/BatchConfig.java` — 청크 기반 `Step` 정의(예: `processOrderStep()`)
- ItemReader
  - `src/main/java/com/example/batch/reader/OrderItemReader.java` — CSV를 읽어 `OrderInputDto`로 변환
- ItemProcessor
  - `src/main/java/com/example/batch/processor/OrderItemProcessor.java` — 유효성 검사 및 도메인(`Order`, `ProcessedOrder`)으로 변환
- ItemWriter
  - `src/main/java/com/example/batch/writer/CompositeOrderWriter.java` — 여러 Writer를 조합하여 실행
  - `src/main/java/com/example/batch/writer/OrderItemWriter.java` — 성공 주문을 DB에 저장
  - `src/main/java/com/example/batch/writer/ProcessedOrderWriter.java` — 추가적으로 가공된 정보를 DB에 저장
- Listeners
  - `src/main/java/com/example/batch/listener/JobCompletionNotificationListener.java` — Job 완료 시 추가 작업(예: 통계 조회, 로그)
  - `src/main/java/com/example/batch/listener/StepExecuteListener.java` — Step 단위 전/후 로그
- Runner / Launcher
  - `src/main/java/com/example/batch/runner/JobRunner.java` — 애플리케이션 시작 시 Job 파라미터 준비 및 Job 실행(입력파일/출력파일 경로 설정)
- DTO / Domain
  - `src/main/java/com/example/batch/dto/OrderInputDto.java` — CSV 컬럼 바인딩 DTO
  - `src/main/java/com/example/batch/domain/Order.java` — DB에 저장되는 엔티티
  - `src/main/java/com/example/batch/domain/ProcessedOrder.java` — DB에 저장되는 엔티티

### 간단한 실행 플로우
1. 애플리케이션 시작(`.\gradlew.bat bootRun` 또는 빌드된 JAR 실행)
2. `JobRunner`가 `input/orders.csv` 존재 유무를 확인(없으면 샘플 생성)하고 `JobParameters`를 만들고 Job을 실행
3. `OrderItemReader`가 CSV를 읽어 `OrderInputDto` 객체를 반환
4. `OrderItemProcessor`가 DTO를 검증/변환. 유효하지 않으면 예외를 발생시키거나 실패 항목으로 표시
5. 성공한 항목은 `CompositeOrderWriter`를 통해 DB에 저장(`OrderItemWriter`, `ProcessedOrderWriter`)
6. Job 완료 후 `JobCompletionNotificationListener`가 실행되어 로그/요약 정보를 남김

## 요구 사항(Prerequisites)

- Java 17 이상(로컬 설치된 JDK)
- Gradle wrapper: 프로젝트에 포함된 `gradlew.bat` 사용 권장
- (권장) Docker & Docker Compose: 로컬에서 MySQL을 빨리 띄우려면 사용
  - `infra/docker-compose.yml`에는 MySQL 서비스와 `infra/mysql/init.sql`을 통한 초기 스키마 설정이 포함되어 있습니다.

> 참고: `build.gradle` 파일에서 컴파일/런타임에 필요한 의존성이 정의되어 있습니다. (일반적으로 `spring-boot-starter-batch`, `spring-boot-starter-data-jpa`, `mysql-connector-java` 등)

## 설정 파일 및 초기화 스크립트

- 애플리케이션 설정: `src/main/resources/application.yml` — 데이터베이스 연결, JPA, 스프링 배치 설정(예: 메타테이블 처리) 등을 정의
- MySQL 초기화 SQL: `infra/mysql/init.sql` — `orders`, `processed_orders` 테이블 생성 스크립트
- Docker Compose: `infra/docker-compose.yml` — MySQL 컨테이너 정의(포트, 볼륨, 초기 SQL 마운트 등)

실행 전에 위 파일들을 확인하여 데이터베이스 URL, 사용자명, 비밀번호가 로컬 환경에 맞는지 조정하세요.

## 샘플 입력/출력 및 CSV 포맷

- 입력 파일(기본): `input/orders.csv`

예상 CSV 헤더(프로젝트의 `OrderInputDto`에 따라 다를 수 있음):
```
order_id,customer_name,product_name,quantity,price,order_date
```
예시 레코드:
```
ORD001,홍길동,노트북,2,1500000,2024-01-15 10:30:00
```
- 날짜 포맷: `yyyy-MM-dd HH:mm:ss` (프로젝트 구현에 따라 다를 수 있으니 `OrderInputDto`와 파서 코드를 확인하세요)
- 주의: CSV 헤더/컬럼명이 `OrderInputDto` 필드와 일치해야 올바르게 바인딩됩니다.

## 빌드 및 실행

1) (선택: MySQL 컨테이너 실행)

```shell
# 작업 디렉터리를 프로젝트 루트에 맞춘 뒤 실행
docker-compose -f infra\docker-compose.yml up -d
```

2) 빌드

```shell
.\gradlew clean build
```

3) 개발용으로 바로 실행

```shell
.\gradlew bootRun
```

4) 또는 빌드된 JAR 실행

```shell
java -jar build\libs\order-spring-batch-0.0.1-SNAPSHOT.jar
```

(추가) Job 파라미터 커스터마이즈 예시

```shell
java -jar build\libs\order-spring-batch-0.0.1-SNAPSHOT.jar --inputFile=input/orders.csv
```

> 참고: `JobRunner`가 기본 입력/출력 파일 경로를 사용하도록 구현되어 있습니다. 필요하면 위 인자나 `application.yml` 값을 수정하세요.

## 문제해결(Troubleshooting)

- 데이터베이스 연결 실패
  - `src/main/resources/application.yml`의 `spring.datasource.url`, `username`, `password`를 확인
  - Docker를 사용했다면 컨테이너가 완전히 기동되었는지 확인(`docker-compose ps`)
- 테이블 없음 오류
  - `infra/mysql/init.sql`을 확인해 스키마가 생성되었는지 확인
  - Spring Batch 메타테이블은 `spring.batch.jdbc.initialize-schema` 설정(보통 `always`)을 통해 자동 생성될 수 있음
- 입력 CSV 관련 오류
  - `input/orders.csv` 파일이 존재하는지 확인. 없다면 `JobRunner`가 기본 샘플을 생성하는지 확인
  - CSV 헤더가 `OrderInputDto`의 필드와 일치하는지 확인
- 로그를 통해 원인 파악
  - `src/main/resources/application.yml`에서 로그 레벨을 `DEBUG`로 올리면 상세 실행 로그를 확인할 수 있습니다

## 확장 가이드 (간단한 예시)

- 새로운 검증/변환 로직 추가
  - `ItemProcessor` 구현체(`OrderItemProcessor`)를 수정하거나 새 구현체를 만들어 `BatchConfig`에 등록
- 추가 출력 대상(예: 외부 API, 다른 테이블)
  - 새로운 `ItemWriter`를 구현하고 `CompositeOrderWriter`에 추가
- 멀티 스텝 Job 구성
  - `BatchConfig`에 새로운 `Step`을 추가하고 `JobBuilder`로 체인 연결

## 실행 로그 샘플
```text
No active profile set, falling back to 1 default profile: "default"
Bootstrapping Spring Data JPA repositories in DEFAULT mode.
Finished Spring Data repository scanning in 9 ms. Found 0 JPA repository interfaces.
HikariPool-1 - Starting...
HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@43aeb5e0
HikariPool-1 - Start completed.
HHH000204: Processing PersistenceUnitInfo [name: default]
HHH000412: Hibernate ORM core version 6.6.41.Final
HHH000026: Second-level cache disabled
No LoadTimeWeaver setup: ignoring JPA class transformer
HHH10001005: Database info:
HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
Initialized JPA EntityManagerFactory for persistence unit 'default'
Started OrderSpringBatchApplication in 1.962 seconds (process running for 2.323)
JobRunner 실행 시작
입력 파일 확인 완료: input/orders.csv
배치 작업 시작 - 입력 파일: input/orders.csv
Job: [SimpleJob: [name=processOrderJob]] launched with the following parameters: [{'inputFile':'{value=input/orders.csv, type=class java.lang.String, identifying=true}','time':'{value=1770216009052, type=class java.lang.Long, identifying=true}'}]
### Job 시작: processOrderJob
### 시작 시간: 2026-02-04T23:40:09.136690300
Executing step: [processOrderStep]
### Step 시작: processOrderStep
### 읽기 카운트: 0
### 쓰기 카운트: 0
OrderItemReader 생성 - 파일 경로: input/orders.csv
CSV 파일 읽기 시작: input/orders.csv
모든 데이터 읽기 완료
데이터 유효성 검증 실패 - Order ID: ORD001, 이유: Price cannot exceed 1,000,000
데이터 유효성 검증 실패 - Order ID: ORD002, 이유: Price cannot exceed 1,000,000
데이터 유효성 검증 실패 - Order ID: ORD004, 이유: Quantity must be greater than 0
ProcessedOrders 테이블에 5개 레코드 저장 완료
Step: [processOrderStep] executed in 109ms
### Step 완료: processOrderStep
### 읽기 카운트: 8
### 쓰기 카운트: 5
### 스킵 카운트: 3
### 처리 시간: 1
========================================
        배치 처리 리포트
========================================
작업 이름: processOrderJob
시작 시간: 2026-02-04T23:40:09.136690300
종료 시간: 2026-02-04T23:40:09.292974300
소요 시간: 0초
----------------------------------------
처리 결과:
  - 성공 처리 건수 (orders): 50
  - 리포트 저장 건수 (processed_orders): 50
  - 총 매출액: 65,500,000원
  - 작업 상태: COMPLETED
----------------------------------------
상품별 판매 현황:
  - 의자: 100개, 30,000,000원
  - 태블릿: 30개, 24,000,000원
  - 키보드: 50개, 7,500,000원
  - 모니터: 10개, 3,000,000원
  - 마우스: 20개, 1,000,000원
========================================
Job 완료 리포트 출력 완료
Job: [SimpleJob: [name=processOrderJob]] completed with the following parameters: [{'inputFile':'{value=input/orders.csv, type=class java.lang.String, identifying=true}','time':'{value=1770216009052, type=class java.lang.Long, identifying=true}'}] and the following status: [COMPLETED] in 156ms
배치 작업 완료
Closing JPA EntityManagerFactory for persistence unit 'default'
HikariPool-1 - Shutdown initiated...
HikariPool-1 - Shutdown completed.
```
