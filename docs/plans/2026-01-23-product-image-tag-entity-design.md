# Product Image & Tag Entity Design

## Goal
상품 이미지(썸네일 1장 + 일반 이미지 N장)와 태그를 위한 데이터 모델을 추가하고, `Product` 엔티티에 연관 관계를 명확히 정의한다. 이 문서는 **엔티티/테이블 구조에만 집중**하며, 저장/조회 흐름은 범위에서 제외한다.

## Scope
- `product_images` 테이블 및 엔티티 추가
- `tags` 테이블 및 엔티티 추가
- `product_tags` 조인 테이블 및 엔티티 추가
- `Product` 엔티티에 이미지/태그 연관 관계 추가

## Entity & Table Design
### product_images
- 필드: `id`(PK), `product_id`(FK), `image_url`(VARCHAR 500), `image_order`(INT), `is_thumbnail`(BOOLEAN), `created_at`, `updated_at`
- 의도: 한 테이블에서 썸네일과 일반 이미지를 모두 관리한다.
- 제약: `product_id`는 `products.id`를 참조한다.
- 인덱스: `product_id`, `(product_id, is_thumbnail)`, `(product_id, image_order)`

### tags
- 필드: `id`(PK), `name`(VARCHAR 100, UNIQUE), `created_at`, `updated_at`
- 의도: 태그명 정규화 및 중복 방지.

### product_tags
- 필드: `id`(PK) 또는 복합키, `product_id`(FK), `tag_id`(FK)
- 제약: `(product_id, tag_id)` 유니크.
- 인덱스: `product_id`, `tag_id`

## JPA Mapping Notes
- `Product` → `ProductImage`: `@OneToMany(mappedBy = "product")`
- `Product` ↔ `Tag`: 명시적 조인 엔티티(`ProductTag`)로 연결
- `ProductImage`, `ProductTag`, `Tag`는 `BaseTimeEntity` 상속

## Out of Scope
- 이미지 업로드/저장 흐름
- 태그 입력/정규화 로직
- API 응답 DTO 수정
