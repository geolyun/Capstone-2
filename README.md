# 링코 (Ringco)

여행 코스를 공유하고 추천받는 플랫폼입니다. 크리에이터가 코스를 등록하면 다른 사용자가 탐색하고, 좋아요를 누르고, 추천을 받을 수 있습니다.

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.5 |
| ORM | Spring Data JPA, Hibernate |
| 동적 쿼리 | QueryDSL 5.1 |
| 보안 | Spring Security, JWT, OAuth2 (Google, Kakao) |
| 뷰 | Thymeleaf |
| DB (개발) | H2 In-Memory |
| DB (운영) | MySQL |
| 파일 저장 | AWS S3 |
| 캐시 | Spring Cache |
| 빌드 | Maven |

---

## 주요 기능

- **코스 관리**: 코스 생성, 수정, 삭제, 검토 요청 / 승인 / 반려
- **검색 및 필터**: 키워드, 카테고리, 지역, 예산, 소요 시간, 태그 기반 동적 검색 (QueryDSL)
- **추천**: 좋아요 기반 연관 코스, 같은 카테고리/지역 코스 추천
- **좋아요**: 코스 좋아요 토글
- **신고**: 코스 신고 접수
- **인증**: 이메일 회원가입(인증 코드), 일반 로그인, Google/Kakao OAuth2 로그인
- **프로필**: 크리에이터 프로필 조회 및 수정
- **관리자**: 신고 목록 조회, 신고 처리, 대시보드 통계
- **이미지 업로드**: AWS S3

---

## 아키텍처

REST API와 서버사이드 렌더링(SSR)을 병행하는 구조입니다.

```
/api/**     → REST API (JSON 응답) - 프론트엔드 연동용
/**         → SSR Controller (Thymeleaf HTML 렌더링) - 브라우저 직접 접근
```

---

## 프로젝트 구조

```
src/main/java/com/capstone/Capstone_2/
├── config/
│   ├── security/           # UserPrincipal (Spring Security 주체)
│   ├── SecurityConfig.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtUtil.java
│   └── OAuth2SuccessHandler.java
├── controller/
│   ├── ssr/                # Thymeleaf SSR 컨트롤러
│   └── *.java              # REST API 컨트롤러
├── dto/                    # 요청/응답 DTO
│   ├── CourseDto.java      # 코스 관련 DTO 묶음 (중첩 클래스/record)
│   └── ...
├── entity/                 # JPA 엔티티
├── repository/             # Spring Data JPA 레포지터리
│   ├── CourseRepositoryCustom.java  # QueryDSL 인터페이스
│   └── CourseRepositoryImpl.java   # QueryDSL 구현체
├── service/
│   ├── course/             # 코스, 좋아요, 신고, 카테고리
│   ├── auth/               # 인증, 이메일
│   ├── user/               # 사용자, OAuth2
│   ├── admin/              # 관리자
│   └── mypage/             # 프로필
└── exception/
    └── GlobalExceptionHandler.java
```

---

## 실행 방법

### 사전 조건

- Java 21
- Maven
- `.env` 파일 (아래 환경변수 필요)

### 환경변수 설정 (`.env`)

```
JWT_SECRET_KEY=your_jwt_secret
AWS_ACCESS_KEY=your_aws_access_key
AWS_SECRET_KEY=your_aws_secret_key
S3_BUCKET_NAME=your_s3_bucket_name
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
```

### 실행

```bash
# 개발 환경 (H2 인메모리 DB)
./mvnw spring-boot:run

# 운영 환경 (MySQL)
./mvnw spring-boot:run -Dspring.profiles.active=prod
```

개발 환경에서는 H2 콘솔(`/h2-console`)을 통해 DB를 확인할 수 있습니다.

---

## 주요 API

| Method | URL | 설명 |
|---|---|---|
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인 (JWT 발급) |
| POST | `/api/auth/verify` | 이메일 인증 |
| GET | `/api/courses` | 코스 검색 |
| POST | `/api/courses` | 코스 생성 |
| GET | `/api/courses/{id}` | 코스 상세 조회 |
| PUT | `/api/courses/{id}` | 코스 수정 |
| DELETE | `/api/courses/{id}` | 코스 삭제 |
| POST | `/api/courses/{id}/submit` | 검토 요청 |
| POST | `/api/courses/{id}/approve` | 승인 (관리자) |
| POST | `/api/courses/{id}/reject` | 반려 (관리자) |
| POST | `/api/courses/{id}/likes/toggle` | 좋아요 토글 |
| GET | `/api/courses/{id}/recommendations` | 코스 추천 |
| GET | `/api/profile/me` | 내 프로필 조회 |
| PUT | `/api/profile/me` | 프로필 수정 |
| POST | `/api/files/upload` | 이미지 업로드 |

---

## DTO 설계 원칙

- **응답(Response) DTO** → `record` 사용 (불변, 읽기 전용)
- **요청(Request) DTO** → `@Getter @Setter @NoArgsConstructor` 클래스 사용 (Jackson 역직렬화, 폼 바인딩 호환)
