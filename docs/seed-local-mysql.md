# 로컬 MySQL 시드 데이터 넣기 (개발 전용)

이 문서는 로컬 개발 환경에서 필터/페이징/성능 테스트용 상품 데이터를 빠르게 채우기 위한 가이드입니다.

## 전제

- `SPRING_PROFILES_ACTIVE=local`
- 로컬 MySQL이 `application-local.yml` 설정대로 실행 중
- `DB_PASSWORD` 환경변수 설정

## 실행 (추천: truncate + confirm)

PowerShell 예시:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
$env:DB_PASSWORD="..."
$env:SEED_ENABLED="true"
$env:SEED_COUNT="10000"
$env:SEED_SEED="42"
$env:SEED_INCLUDE_IMAGES="true"
$env:SEED_TRUNCATE="true"
$env:SEED_CONFIRM="YES"
cd .\cherry-server
.\gradlew bootRun
```

## 옵션

- `seed.enabled` (`SEED_ENABLED`): `true`일 때만 실행
- `seed.count` (`SEED_COUNT`): 생성할 상품 수 (기본 10000)
- `seed.seed` (`SEED_SEED`): 랜덤 시드 (재현용, 기본 42)
- `seed.include-images` (`SEED_INCLUDE_IMAGES`): placeholder 이미지 row 생성 여부 (기본 true)
- `seed.truncate` (`SEED_TRUNCATE`): 실행 전 테이블 비움 (기본 false)
- `seed.confirm` (`SEED_CONFIRM`): `truncate=true`일 때 반드시 `YES`

## 주의

- `SeedRunner`는 `local` 프로필에서만 로딩됩니다.
- `truncate=true`는 로컬 DB 데이터가 모두 삭제됩니다.

