# AURA Service Server

클라우드 데이터 정리 및 디지털 탄소 절감 서비스 **AURA**의 백엔드 서버입니다.  
Google OAuth 로그인, JWT 인증, Gmail/Google Drive 권한 관리, 메타데이터 스캔, Gemini API 기반 AI 분석, 정리 작업, 저장소 관리, 통계, 알림 기능을 담당합니다.

AURA는 사용자의 Gmail과 Google Drive에 누적된 오래된 메일, 광고성 메일, 중복 파일, 대용량 파일 등을 분석하고, 사용자의 최종 승인 후 안전하게 정리 작업을 수행합니다.

## 담당 범위

- Google OAuth 기반 로그인 및 신규 사용자 자동 가입
- Access Token / Refresh Token 발급, 재발급, 로그아웃 처리
- JWT 기반 사용자 인증 필터 및 Security 설정
- 사용자 동의 상태 조회/수정 및 회원 정보 관리
- Gmail/Google Drive 권한 상태 조회, 재검증, 재연결 URL 발급
- Gmail/Drive 메타데이터 스캔 및 스캔 작업 상태 관리
- Gemini API 기반 AI 의미 분석 및 Decision Engine 후보 분류
- 정리 후보 조회 및 선택 상태 변경
- 사용자가 승인한 항목의 휴지통 이동, 복구, 영구 삭제, 휴지통 비우기 처리
- 정리 이력, 확보 용량, 탄소 절감량 통계 제공
- FCM 기반 알림 설정, 토큰 등록, 알림 내역 관리
- Swagger 기반 API 문서화 및 공통 응답/예외 처리 구조 설계

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Security | Spring Security, JWT(JJWT), Google OAuth 2.0 |
| Database | AWS RDS MySQL 8.0, Spring Data JPA |
| AI | Gemini API |
| External API | Gmail API, Google Drive API |
| Notification | Firebase Cloud Messaging |
| Docs | Springdoc OpenAPI / Swagger UI |
| Deploy | AWS EC2, Docker, Nginx |
| Test | JUnit 5, Spring Boot Test |

## 핵심 구현

### 1. Google OAuth 로그인과 JWT 인증

클라이언트가 전달한 Google 인증 결과 또는 Authorization Code를 서버에서 검증한 뒤 사용자 정보를 조회합니다.  
신규 사용자인 경우 `users`, `oauth_tokens`, `user_consents`, `notification_settings` 데이터를 생성하고 AURA 서비스용 JWT 토큰을 발급합니다.

로그인 이후 보호 API는 `Authorization: Bearer {access_token}` 형식의 JWT 인증을 사용합니다.

### 2. 사용자 동의 및 Google 권한 관리

AURA는 Gmail과 Google Drive 메타데이터를 다루기 때문에 개인정보 처리, AI 분석, 메타데이터 활용, 사용자 승인 기반 정리 안내에 대한 동의 상태를 관리합니다.

또한 Gmail/Drive 권한 상태를 저장하고, 권한 만료 또는 해제 상황에서 재검증 및 재연결 URL 발급을 지원합니다.

### 3. Gmail/Drive 스캔과 AI 분석

스캔 작업이 생성되면 서버는 Gmail API와 Google Drive API를 통해 메타데이터를 수집합니다.  
메일 본문 전체와 Drive 파일 원문은 저장하지 않으며, 분석에 필요한 제목, 일부 스니펫, 파일명, MIME 타입, 크기, 날짜, 폴더 경로 등 제한된 메타데이터만 사용합니다.

수집된 메타데이터는 Gemini API로 전달되며, 응답 결과는 Spring Boot 내부 Decision Engine에서 검증됩니다.  
Decision Engine은 보호 규칙, 중복 탐지, 사용자 조건, 기간 조건 등을 함께 적용하여 최종 정리 후보를 분류합니다.

### 4. 정리 작업 및 저장소 관리

사용자가 최종 승인한 항목에 대해서만 정리 작업을 생성합니다.  
정리 작업은 `cleanup_jobs`와 `cleanup_job_items`를 기반으로 관리하며, 항목별 처리 상태를 저장합니다.

지원하는 정리 작업은 다음과 같습니다.

- Gmail/Drive 항목 휴지통 이동
- 휴지통 항목 복구
- 휴지통 항목 영구 삭제
- 휴지통 비우기
- 정리 작업 상태 및 결과 조회
- 실패 항목 재시도

저장소 화면에서는 Gmail API와 Google Drive API를 통해 최신 저장소 목록과 휴지통 목록을 조회합니다.

## 주요 API

### Auth

| Method | Endpoint | 설명 |
| --- | --- | --- |
| POST | `/api/auth/google/login` | Google OAuth 로그인 및 JWT 발급 |
| POST | `/api/auth/token/refresh` | AURA JWT 재발급 |
| POST | `/api/auth/logout` | 로그아웃 |

### User

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/users/me` | 내 정보 조회 |
| GET | `/api/users/me/privacy-data` | 개인정보 데이터 조회 |
| POST | `/api/users/me/withdrawal` | 서비스 탈퇴 |

### User Consent

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/user/consents` | 사용자 동의 상태 조회 |
| PUT | `/api/user/consents` | 사용자 동의 상태 수정 |

### Google Permission

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/google/permissions` | Gmail/Drive 권한 상태 조회 |
| POST | `/api/google/permissions/recheck` | Google 권한 재검증 |
| POST | `/api/google/permissions/reconnect-url` | 권한 재연결 URL 발급 |
| DELETE | `/api/google/connection` | Google 계정 연결 해제 |

### Scan

| Method | Endpoint | 설명 |
| --- | --- | --- |
| POST | `/api/scans` | Gmail/Drive 스캔 시작 |
| GET | `/api/scans/running` | 진행 중인 스캔 조회 |
| GET | `/api/scans/{scan_job_id}` | 스캔 상세 조회 |
| POST | `/api/scans/{scan_job_id}/cancel` | 스캔 취소 |
| GET | `/api/scans/history` | 스캔 이력 조회 |

### Candidate

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/scans/{scan_job_id}/analysis-summary` | 분석 요약 조회 |
| GET | `/api/scans/{scan_job_id}/candidates` | 정리 후보 목록 조회 |
| GET | `/api/candidates/{candidate_id}` | 정리 후보 상세 조회 |
| PATCH | `/api/candidates/{candidate_id}/selection` | 후보 선택 상태 변경 |
| PATCH | `/api/scans/{scan_job_id}/candidates/selection` | 후보 일괄 선택 상태 변경 |
| GET | `/api/scans/{scan_job_id}/selected-candidates` | 선택된 후보 조회 |

### Cleanup

| Method | Endpoint | 설명 |
| --- | --- | --- |
| POST | `/api/cleanup-jobs` | 정리 작업 생성 |
| POST | `/api/cleanup-jobs/{cleanup_job_id}/start` | 정리 작업 수동 시작 |
| GET | `/api/cleanup-jobs/{cleanup_job_id}` | 정리 작업 상태 조회 |
| GET | `/api/cleanup-jobs/{cleanup_job_id}/items` | 정리 작업 항목 조회 |
| GET | `/api/cleanup-jobs/{cleanup_job_id}/result` | 정리 작업 결과 조회 |
| POST | `/api/cleanup-jobs/{cleanup_job_id}/retry-failed` | 실패 항목 재시도 |

### Storage

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/storage/items` | 저장소 항목 목록 조회 |
| GET | `/api/storage/items/{item_id}` | 저장소 항목 상세 조회 |
| GET | `/api/storage/items/detail` | 실시간 저장소 항목 상세 조회 |
| GET | `/api/storage/trash` | 휴지통 항목 목록 조회 |
| POST | `/api/storage/trash/restore` | 휴지통 항목 복구 |
| POST | `/api/storage/trash/permanent-delete` | 휴지통 항목 영구 삭제 |
| POST | `/api/storage/trash/empty` | 휴지통 비우기 |

### Statistics

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/statistics/summary` | 통계 요약 조회 |
| GET | `/api/statistics/monthly` | 월별 통계 조회 |
| GET | `/api/statistics/cleanup-histories` | 정리 이력 조회 |
| GET | `/api/statistics/carbon-formula` | 탄소 절감 계산식 조회 |

### Notification / Announcement

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/notifications/settings` | 알림 설정 조회 |
| PUT | `/api/notifications/settings` | 알림 설정 수정 |
| POST | `/api/notifications/fcm-token` | FCM 토큰 등록 |
| GET | `/api/notifications` | 알림 목록 조회 |
| PATCH | `/api/notifications/{notification_id}/read` | 알림 읽음 처리 |
| GET | `/api/announcements` | 공지사항 목록 조회 |
| GET | `/api/announcements/{announcement_id}` | 공지사항 상세 조회 |
| POST | `/api/announcements/{announcement_id}/read` | 공지사항 읽음 처리 |

## 프로젝트 구조

```text
src/main/java/com/AURA/AURA_Service/
├── auth          # 인증, 사용자, 권한, 알림, 공지사항 도메인
├── scan          # 스캔 작업, 메타데이터 수집, AI 분석, 후보 관리
├── cleanup       # 정리 작업 생성, 실행, 결과 관리
├── storage       # 저장소 목록, 상세, 휴지통 관리
├── statistics    # 정리 통계 및 탄소 절감량 계산
├── home          # 홈 요약 정보
├── global        # 공통 응답, 예외, 설정
└── config        # Security, Swagger 등 설정
