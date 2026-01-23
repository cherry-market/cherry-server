# Local MySQL Seed Data Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use `executing-plans` to implement this plan task-by-task.

**Goal:** 로컬 MySQL(`application-local.yml`)에 필터/페이징/성능 테스트용 상품 데이터 1만 건(+연관 데이터)을 안전하게 주입하는 시드 도구를 만든다.

**Architecture:** 운영 코드에 영향을 주지 않도록, `local` 프로필에서만 동작하고 명시적인 플래그가 있어야만 실행되는 “개발용 seeder”를 만든다. 데이터 생성은 재현 가능(seed 고정)하고, 분포(카테고리/가격/상태/거래방식/태그/시간)를 제어한다.

**Tech Stack:** Spring Boot 3, MySQL, `JdbcTemplate`(대량 insert), `@Profile("local")`, `@ConditionalOnProperty`.

---

## Options Review (선택지 비교)

1) **SQL 파일로 대량 INSERT**
   - 빠르지만 관계 데이터(태그/이미지) 분포 제어가 어렵고 재실행/파라미터화가 불편.

2) **Seeder 스크립트/Runner (추천)**
   - 분포 제어/재현성/안전장치(로컬 전용/플래그) 구현이 쉽고, 1만~10만까지 확장 가능.

3) **Admin API로 생성**
   - “실수로 prod 실행” 리스크가 커서 포트폴리오/개발용에는 비추천.

---

## Task 1: Seed 실행 방식 결정 (Runner vs Standalone)

**Decision:** Spring Boot 앱 실행 시점에 붙는 Runner로 구현하되, 기본은 비활성.

**Execution UX (예시):**
- `SPRING_PROFILES_ACTIVE=local SEED_ENABLED=true SEED_COUNT=10000 SEED_SEED=1234 ./gradlew bootRun`
- 기본값: `SEED_ENABLED` 없으면 절대 실행 안 함.

---

## Task 2: Seeder 골격 추가 (안전장치 포함)

**Files:**
- Create: `src/main/java/com/cherry/server/dev/seed/SeedRunner.java`

**Requirements:**
- `@Profile("local")`
- `@ConditionalOnProperty(name = "seed.enabled", havingValue = "true")`
- 입력 파라미터(환경변수/프로퍼티):
  - `seed.count` (default 10000)
  - `seed.seed` (default 42) — 랜덤 고정
  - `seed.includeImages` (default true/false)
  - `seed.truncate` (default false) — true면 관련 테이블 비우고 시작(로컬 전용)
- “실수 방지”를 위해 `seed.truncate=true` 일 때도 `seed.confirm=YES` 같은 확인값 없으면 종료.

---

## Task 3: 데이터 분포(Variation) 정의

**Targets (예시):**
- Categories: 8~12개 (패션굿즈 포함/제외는 선택; 포함해도 이미지 없음으로 처리 가능)
- Sellers: 50~200명
- Products: 10,000개
- Status 비율: SELLING 70%, RESERVED 20%, SOLD 10%
- TradeType 비율: DIRECT 40%, DELIVERY 40%, BOTH 20%
- Price 분포: 저가/중가/고가 구간 혼합 (예: 1천~50만, 로그/구간 랜덤)
- createdAt 분포: 최근 180일 내 랜덤 (정렬/커서 테스트용)
- Tags: 태그 풀 100~300개, 상품당 0~5개
- Images:
  - 전체: 썸네일 1장(placeholder URL)만
  - 앨범 카테고리: 상세 이미지 3장 추가(placeholder URL)
  - 실제 파일은 불필요(테스트 데이터)

---

## Task 4: 대량 Insert 구현 (JdbcTemplate + Batch)

**Files:**
- Modify: `src/main/java/com/cherry/server/dev/seed/SeedRunner.java`

**Approach:**
- `JdbcTemplate.batchUpdate(...)`로 테이블별 batch insert
- FK 순서: `users` → `categories` → `tags` → `products` → `product_images` → `product_tags`
- 기존 데이터와 충돌 방지:
  - `truncate=false`면 `MAX(id)+1`부터 id 시작(명시 id insert) 또는 이메일/코드 기반 upsert
  - `truncate=true`면 관련 테이블 정리 후 1부터 시작

**Verification Output:**
- 생성 후 간단 통계 출력(콘솔):
  - 카테고리별 상품 수
  - status/tradeType 분포
  - 가격 min/max/avg

---

## Task 5: 로컬 실행 가이드 문서화

**Files:**
- Create: `docs/seed-local-mysql.md`

**Runbook:**
- 필요한 환경변수(`DB_PASSWORD`, `SPRING_PROFILES_ACTIVE=local`)
- 예시 커맨드
- truncate 모드 주의사항

---

## Task 6: (선택) 성능 측정 스크립트

**Goal:** Phase 4(캐싱)로 넘어가기 전에 “느린 상태” 기준 수치 확보를 자동화.

**Approach:**
- `curl`/`hey`/`k6` 중 하나로 `GET /products` (필터+커서 조합) 호출해 p95/평균 기록
- 결과를 `docs/perf/before.md` 형태로 저장
