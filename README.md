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

## 도메인 설계
### User
- User는 상태(State)를 중심으로 동작하는 상태 머신(State Machine)입니다
- 모든 계정 행위는 현재 상태(Status)에 따라 허용 / 차단된다

#### 책임 분리
- Service는 상태 전이 여부를 판단
- 실제 상태 변경은 User 엔티티 내부 메서드로만 수행
- 이를 통해 상태 변경 규칙이 분산되지 않도록 설계

#### 상태 
```text
ACTIVATION    : 정상 사용자
LOCKED        : 로그인 실패 등 보안 정책에 의해 잠긴 상태
INACTIVATION  : 탈퇴(Soft Delete)된 사용자
```
#### 상태 전이
ACTIVATION  
├─(로그인 실패 임계 초과)→ LOCKED  
├─(회원 탈퇴)→ INACTIVATION  

LOCKED  
├─(관리자 unlock)→ ACTIVATION  

INACTIVATION  
├─(reJoin + 본인 검증)→ ACTIVATION  


#### 주요 정책
- 로그인 실패 시 실패 횟수 증가
- 실패 횟수 임계치 초과 시 LOCKED 상태 전이
- 관리자에 의한 unlock 가능
- 탈퇴(INACTIVATION)는 Soft Delete로 처리된다
- 계정 정보는 DB에 유지되며, 재가입(rejoin)을 통해 본인 검증 후 재활성화할 수 있다
- 재가입(rejoin)은 새로운 계정을 생성하는 것이 아니라,  
  기존 계정에 대한 본인 검증 (이메일 + 실명)을 거친 뒤  
  상태를 ACTIVATION으로 되돌리는 방식이다

#### 설계 특징
- 상태 전이는 User 엔티티 내부 메서드로만 가능하게 만들었다
- Service는 상태를 "판단"만 하고, 실제 변경은 도메인이 수행한다
- 보안 정책 변경 시 도메인 규칙만 수정하면 된다

#### 보안 관점
- 관리자 계정 생성은 일반 회원 가입 플로우에 노출되지 않는다
- 관리자 계정은 별도 초기화 로직 또는 제한된 경로를 통해서만 생성된다
- 로그인 실패 횟수, 잠금, 해제 등 보안 정책의 최종 결과는  
  모두 User 상태 전이로 귀결되도록 설계했다

---
### Board
- Board는 게시글을 담는 **정책 단위 도메인**이다
- 게시판의 성격(Type)에 따라 허용되는 행위가 다르며,  
  이는 도메인 내부 규칙으로 관리된다

#### 책임 분리
- Controller는 접근 권한(관리자 여부)을 검증한다
- Service는 요청을 위임하고 트랜잭션을 관리한다
- 게시판 정책(수정 가능 여부 등)은 Board 엔티티가 직접 판단한다

#### 게시판 타입
```text
TEMPORARY : 임시 게시판 (수정/삭제 제한)
FREE      : 일반 게시판
TEST      : 테스트용 게시판
```

#### 주요 정책 
- `TEMPORARY` 타입의 게시판은 수정이 불가능하다
- 게시판 수정 가능 여부는 Service가 아닌 도메인에서 판단한다
- 관리자만 게시판 삭제가 가능하다

#### 설계 특징
- 게시판 정책을 Service 분기가 아닌 도메인 메서드로 캡슐화
- 게시판 타입별 규칙이 분산되지 않도록 설계
- 새로운 게시판 타입이 추가되더라도 기존 로직 변경 없이 확장 가능

#### DTO 설계
- 게시판 요청 DTO는 sealed interface + record 조합으로 구성
- Board 관련 요청의 범위를 명확히 제한하여 타입 안정성을 확보
- 요청 검증 책임을 DTO 계층으로 한정  

**이 프로젝트의 도메인은 상태와 정책을 스스로 책임지는 객체로 설계되었다**

---
### Post 
- Post는 게시판(Board)에 종속된 컨텐츠 도메인이다
- 게시글은 작성자(User)와 게시판(Board)을 반드시 가진다

#### 책임 분리
- Controller는 인증된 사용자 정보를 전달
- Service는 유스케이스 흐름과 트랜잭션을 관리
- 권한 검증은 `AuthorizationService`에 위임
- 게시글 수정/삭제 정책은 도메인 규칙에 따른다

#### 주요 정책
- 게시글 작성은 USER 권한만 가능하다
- 공지글 작성은 ADMIN 권한만 가능하다
- 게시글 수정/삭제는 작성자 본인만 가능하다 (관리자는 타인의 게시글 수정 불가)
- 관리자는 모든 게시글에 대한 삭제 권한을 가진다
- 수정/삭제와 같은 민감 행위는 비밀번호 재입력을 요구한다

#### 조회 전략
- Offset 기반 페이징 : 일반 게시판 조회에 사용
- Cursor 기반 페이징 : 대량 데이터 및 무한 스크롤 대응

#### 설계 특징
- 권한 판단 로직을 `AuthorizationService`로 분리
- Service 계층에 권한 분기 로직이 흩어지지 않도록 설계
- 게시글과 공지글을 엔드포인트 및 생성 로직에서 명확히 분리

#### DTO 설계
- Post 요청 DTO는 sealed interface + record로 구성
- BoardRequest를 상속하여 게시글-게시판 관계를 타입으로 표현
- 요청 검증 책임을 DTO 계층으로 한정

---
### Comment
Comment는 게시글(Post)에 종속되는 하위 도메인,  
의사 표현의 최소 단위이며 상태를 가지지 않는 단순 엔티티  

User/Post와 달리 독립적인 생명주기를 갖지 않고,  
**게시글과 사용자에 강하게 결합된 값 객체 성격**으로 설계되었습니다  

#### 책임
- 게시글에 대한 의견 표현
- 작성자(User)와 게시글(Post)간의 연결
- 수정,삭제 시 작성자 및 관리자 권한 검증

#### 설계 원칙
- Comment는 상태(State)를 갖지 않는다
- 활성/잠금/탈퇴 같은 상태 관리는 User의 책임이다
- Comment 자체는 "존재한다/삭제된다"만 판단

#### 권한 정책 
##### 생성 (Create)
- 로그인한 사용자 (USER, ADMIN)만 작성 가능
- 게시글(Post)의 정책에 따라 댓글 작성 가능 여부를 판단
- 댓글 작성 시 현재 인증된 사용자 기준으로 작성자가 결정

##### 수정(Update)
- 작성자 본인만 수정 가능
- 관리자(ADMIN)는 댓글을 수정할 수 없음 (문제 시 삭제만 가능)
- 수정 시 현재 로그인 사용자 기준 비밀번호 재확인

##### 삭제(Delete)
- 작성자 본인 또는 관리자(ADMIN)만 삭제 가능
- 관리자 삭제 시에도 관리자 비밀번호 재확인 수행
- 일괄 삭제(관리자 전용) 기능 제공

#### 보안 정책 
- 모든 수정/삭제 요청은 **현재 로그인한 사용자 기준**으로 검증
- 요청 Body에 포함된 정보만으로 권한 판단을 하지 않는다 
- 관리자 기능은 `@PreAuthorize`를 통해 접근 자체를 제한

#### 연관 관계 관리
```text
Post (1) ──── (N) Comment
User (1) ──── (N) Comment
```
- Comment는 Post와 User 양쪽에 모두 종속된다
- 삭제 시 연관관계 컬렉션에서 제거하여  
  영속성 컨텍스트와 DB 상태를 명확히 동기화한다 (추후 Comment 엔티티에 헬퍼 메서드를 둬서 한번에 처리 가능하게 할 예정)
- orphanRemoval = true를 활용해 고아 객체를 자동 삭제 

#### 설계 특징
- 값 검증과 비즈니스 검증을 명확히 분리
- 권한 판단은 Service 레벨에서,  
  실제 데이터 변경은 엔티티 내부에서 수행
- 관리자 기능과 일반 사용자 기능을 논리적으로 분리하여  
  보안 정책 변경에 유연하게 대응 가능  

Comment는 상태를 갖지 않는 하위 도메인으로, **권한·보안 정책을 중심으로 User/Post와 일관되게 설계된 엔티티**

---
위 도메인 들은 모두 "인증된 사용자"와 "권한"을 전제로 동작한다  
아래에서는 이러한 도메인 동작을 가능하게 하는  
Authentication / Authorization 구조를 설명한다  

---
## Authentication & Authorization 설계 (인증 & 인가)
이 프로젝트는 인증(Authentication)과 인가(Authorization)를  
서로 다른 책임으로 분리하여 설계했다  

인증은 "이 요청이 누구로부터 왔는가"를 판단하는 과정이고,  
인가는 "이 사용자가 이 행위를 할 수 있는가"를 판단하는 과정이다

### Authentication (인증)
- JWT 기반 인증 (Access Token / Refresh Token)
- Stateless Security Filter Chain 구조 
- 인증 실패는 Controller까지 요청을 전달하지 않는다 

#### 처리 흐름 
```text
Client Request
→ JwtAuthenticationFilter
→ (성공) SecurityContext에 Authentication 저장
→ (실패) AuthenticationEntryPoint 호출
```
#### 설계 의도
- 토큰 파싱/서명/만료 검증은 Filter 책임
- 인증 성공 시에만 `SecurityContext`에 Authentication이 채워진다
- 인증 실패는 `AuthenticationEntryPoint`에서 일괄 처리한다

---
### Authorization (인가)
인가는 인증이 완료된 사용자에 대해  
"이 사용자가 해당 리소스에 접근할 수 있는가"를 판단하는 단계이다

#### 인가 전략
```text
1차 인가 : Spring Security (@PreAuthorize)
2차 인가 : AuthorizationService + Domain Rule
```
- URL 접근 자체는 Spring Security에서 차단 
- 비즈니스 권한 판단은 AuthorizationService에 위임
- Service 계층에 권한 분기 로직이 흩어지지 않도록 설계 

### 인증 실패와 인가 실패 분리
이 프로젝트는 인증 실패와 인가 실패를  
서로 다른 보안 사건으로 취급한다 

| 구분    | 의미       | 처리 위치                    |
|-------|----------|--------------------------|
| 인증 실패 | 신원 확인 실패 | AuthenticationEntryPoint |
| 인가 실패 | 권한 부족    | AccessDeniedHandler      |

- 인증 실패는 보안 경계에서 즉시 차단
- 인가 실패는 인증된 사용자에 대한 정책 위반
- 두 케이스는 서로 다른 Audit 이벤트로 기록된다

---
인증과 인가는 단순히 요청을 허용/차단하는 것으로 끝나지 않는다
이 프로젝트에서는 모든 보안 판단의 결과를  
감사(Audit) 이벤트로 기록한다

## Security Audit Logging 
이 프로젝트는 인증/인가 결과를  
보안 이벤트(Audit)로 기록한다

- AuthenticationEntryPoint → 인증 실패 Audit
- AccessDeniedHandler → 인가 실패 Audit
- AuthService / Domain → 보안 정책 판단 결과 Audit

### 설계 특징
- 인증 / 인가 이벤트를 DB에 Audit 로그로 저장
- Severity 기반 이벤트 분류 (INFO / WARNING / CRITICAL)
- ClientContext를 통한 요청 환경 정보 분리

### Account Security
Account Security는 User 도메인의 상태(State)와 직접적으로 연결되는 보안 정책을 담당한다  

- 로그인 실패 횟수 기반 계정 잠금
- 관리자에 의한 계정 unlock 가능 (관리자 전용 API)

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
- 관리자 보안 이벤트 대시보드
- IP 기반 이상 행위 감지 