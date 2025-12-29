# Board Application 
게시판 만들기  

이 프로젝트는 Spring Security + JWT 기반의 인증/인가 구조를 사용했습니다  
보안 안정성과 설계 명확성을 우선으로 하여 단일 Refresh Token 전략 + Rotation + 재사용 감지를 적용했습니다.


## Tech Stack
- Java 17+
- Spring Boot
- Spring Security
- JPA (Hibernate)
- JWT (Access / Refresh Token)
- H2 / MySQL

## 핵심 기능

### Authentication / Authorization
- JWT 기반 인증 (Access / Refresh Token)
- Refresh Token 재사용 감지 
- Stateless Security Filter Chain

### Security Audit Logging
- 인증 / 인가 이벤트를 DB에 Audit 로그로 저장
- Severity 기반 이벤트 분류 (INFO / WARNING / CRITICAL)
- ClientContext를 통한 요청 환경 정보 분리 

### Account Security
- 로그인 실패 횟수 기반 계정 잠금
- 관리자에 의한 계정 unlock 가능 

---

## 설계 의도 
### 왜 HttpServletRequest를 Service로 넘기지 않았는가? 
- Service 계층이 웹 기술에 의존하지 않도록 하기 위함
- Filter에서 ClientContext DTO로 요청 환경 정보 추출
- Service는 비즈니스 판단만 수행

### 왜 로그(log)와 Audit(SecurityEvent)를 분리했는가? 
- log : 운영/디버깅 목적
- Audit : 보안 분석/증거/관리자 조회 목적

### 왜 SecurityEventType에 Severity를 고정했는가? 
- 이벤트 의미와 심각도를 분리하지 않기 위함
- 잘못된 심각도 기록을 구조적으로 방지

---

## 보안 흐름
Client Request  
→ ClientContextFilter (요청 환경 수집)  
→ JwtAuthenticationFilter (토큰 검증)  
→ AuthService (보안 판단)  
→ SecurityEventService (Audit 기록)  

---

## 향후 확장
- Redis 기반 로그인 실패 횟수 제한 (Time Window)
- CAPTCHA 연동
- 관리자 보안 이벤트 대시보드ㅡ
- IP 기반 이상 행위 감지 