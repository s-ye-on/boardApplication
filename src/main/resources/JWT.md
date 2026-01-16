# JWT 기반 비교

## JWT란?
JWT(Json Web Token)은 사용자의 인증 정보를 "토큰"에 담아서 클라이언트가 직접 들고 다니는 방식

### ✔ 핵심 아이디어
- 사용자가 로그인하면 서버가 JWT를 발급
- 클라이언트(브라우저,앱)가 JWT를 로컬에 저장
- 요청할 때마다 JWT를 HTTP Header에 포함
- 서버는 그 토큰이 진짜인지 검증만 하고 세션 저장X
즉, 인증 정보를 서버가 아니라 클라이언트 쪽에 저장하는 방식

### JWT 인증의 특징
#### ✅장점
- 세션/서버 저장소가 필요 없음(Stateless)
- 서버 확장(Scale-out)에 유리
- 빠름 (서버는 토큰만 검증하면 됨)
- 모바일/SPA(React, Vue)에서 많이 사용

#### ❌단점
- 발급된 토큰은 유효 기간 동안 강제로 무효화시키기 어려움
- 토큰 탈취되면 위험
- 토큰이 커서 네트워크 비용 증가

### Spring Security는 인증 방식이 아니다
✔ Spring Security = 프레임워크 </br>
사용자 인증, 인가, 권한 체크, 필터 체인 등을 관리해주는 보안 프레임워크야.

✔ JWT = 인증 수단 중 하나 </br>
Spring Security에서 사용할 수 있는 여러 인증 방식 중 하나일 뿐.

즉,
•	Basic 인증
•	Form Login
•	OAuth2
•	JWT 인증
•	세션 기반 인증

이런 여러 인증 방식 중 JWT 방식을 Spring Security가 처리할 수 있게 해주는 구조야.

### 두 개의 관계를 그림으로 보면
```java
    사용자 요청
        ↓
  [Spring Security Filter Chain]
        ↓
  (JWT 필터에서 토큰 검증)
        ↓
 SecurityContextHolder에 인증 정보 저장
        ↓
   Controller 로직 실행
```
즉, Spring Security는 틀(Framework) </br>
JWT는 그 틀 안에서 사용하는 인증 방식(Strategy)

### JWT 기반 인증을 사용하면 Spring Security는 이렇게 동작
1. 사용자가 로그인 요청 전송
   - 서버는 username/password를 확인
   - 정보가 맞으면 JWT 생성 후 리턴
2. 클라이언트가 JWT를 저장
   -(localStorage, sessionStorage, cookie 등)
3. 요청 시 Authorization Header에 붙여서 보냄
```java
Authorization: Bearer <JWT>
```
4. Spring Security 필터 체인 중에서 JWT 필터가 가동
   - 토큰 검증
   - 토큰에서 userId, role 꺼내기
   - SecurityContext에 인증 정보 저장
5. 이후의 권한 체크는 Spring Security가 처리
    Controller에서는 @AuthenticationPrincipal 등으로 유저 정보 접근 가능

### JWT 방식과 가장 큰 차이는 "서버가 상태를 가지냐(세션) / 안가지냐(JWT)"

## 현재까지 만들어진 것 (세션 기반 로그인)

현재 :
- 로그인 성공 -> Spring Security가 세션을 만들고 JSESSIONID 쿠키 발급
- 이후 요청 -> 쿠키로 자동 인증, SecurityContext 에 CustomUserDetails 들어 있음

JWT 로 바꾸면 :
- 로그인 성공 -> 서버가 JWT(access token + 필요함녀 refresh token)발급
- 클라이언트는 JWT를 Authorization: Bearer <token> 헤더로 보냄
- 서버는 세션 안쓰고 매 요청마다 토큰 검증해서 SecurityContext에 Authentication 세팅

바뀌는 포인트:
- SecurityConfig에서 세션 사용 대신 JWT 필터 추가
- 로그인 엔드포인트에서 세션을 만들지 않코 토큰 ㄴ발급
- 컨트롤러/서비스 코드는 그대로 @AuthenticationPrincipal / SecurityContext를 사용
- -> 구조는 유지

## JWT 설계 결정할 것들

### 토큰 종류
보통 :
- Access Token
    - 짧은 만료 시간 (예: 15분 ~1시간)
    - 매 요청마다 Authorization: Bearer <access_token>으로 전송
- Refresh Token (선택)
    - 더 긴 만료 시간 (며칠~ 몇 주)
    - Access Token이 만료되면, Refresh Token으로 새 Access Token 재발급
    - 서버 DB/Redis에 refresh token을 저장하고 관리(로그아웃/강제 만료 대응)

### 토큰에 담을 정보 (Claims)
최소한:
- sub(subject) : 사용자 고유 ID(예 : userId)
- 우리는 User.id
    - email : 로그인 아이디
    - role : USER / ADMIN
    - exp : 만료 시간(expiration)

예 : JWT payload 예시 
```java
{
  "sub": "1",
  "email": "gildong@example.com",
  "role": "USER",
  "exp": 1733300000
}
```
CustomUserDetails를 그대로 쓰고 있으니, 토큰 파싱 후 : 
- id -> CustomUserDetails 생성에 사용
- email, role -> 함께 세팅

### 어디에 토큰을 저장할지
- SPA/모바일 + API 기준으로는 보통 : 
  - Authorization: Bearer <token> 헤더에 넣어서 전송
- 서버 사이 세션 필요 없음, 프론트에서 토큰만 기억하면 됨 </br>
  (브라우저 + XSS/CSRF 보안까지 엄밀히 들어가면 좀 복잡해짐..)

## 기술 설계 : Spring Security + JWT
추가로 만들 것들 : 
1. JWT 유틸/Provider (JwtTokenProvider 같은 클래스)
   - 토큰 생성 : String generateToken(CustomUserDetails userDetails)
   - 토큰 파싱/검증 : getUserIdFromToken, validateToken
2. JWT 인증 필터 (JwtAuthenticationFilter - OncePerRequestFilter 상속)
   - 매 요청마다 Authorization:Bearer ... 헤더 파싱
   - 유효하면 Authentication 만들어서 SecurityContext에 넣기
3. SecurityConfig 수정
   - formLogin / 세션 기반 대신, JwtAuthenticationFilter를 필터 체인에 등록
   - sessionManagement().sessionCreationPolicy(STATELESS) 설정
4. 로그인 API 변경
   - /auth/login 같은 엔티 포인트를 만들어서 : 
     - 이메일/비번 확인
     - JwtTokenProvider로 access token 발급
     - {"accessToken":"..."} 형태로 응답

컨트롤러/ 서비스에서의 @AuthenticationPrincipal CustomUserDetails 사용은 </br>
-> JwtAuthenticationFilter가 토큰을 파싱해서 Authentication을 만들어주면 그대로 동작함

## 구현 단계 계획
1. JWT 유틸 클래스 설계/구현
   - 비밀키 관리 (예: application.yml 에 jwt.secret / jwt.expiration 설정)
   - access token 생성/검증 메서드 작성
2. JwtAuthenticationFilter 구현
   - OncePerRequestFilter 상속
   - Authorization 헤더에서 Bearer 토큰 추출
   - JwtTokenProvider로 검증 + Authentication 생성 -> SecurityContextHolder에 세팅
3. SecurityConfig 토큰 기반으로 전환
   - .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS) 추가
   - .formLogin().disable();, .logout().disable(); (필요에 따라)
   - .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
4. 로그인 API 리디자인
   - POST /auth/login (JSON: { "email": "...", "password": "..." })
   - 성공 시: { "accessToken": "..." } 반환
   - 클라이언트는 이후 모든 요청에 Authorization: Bearer <token> 헤더 추가
5. 테스트 흐름
   - 회원가입 → /auth/login 으로 토큰 받기 → Postman/브라우저에서 헤더에 토큰 달고 /boards, /comments 호출

## 설계 시작
설정 값 (application.yml)
```yaml
app:
  jwt:
    secret: very-secret-jwt-key-change-me-please-very-long
    expiration-seconds: 3600  # 1시간 (원하는 값으로)
```
build.gradle의 dependencies 블록에 : 
```java
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'jakarta.validation:jakarta.validation-api:3.1.0'
    implementation 'org.hibernate.validator:hibernate-validator:8.0.0.Final'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.security:spring-security-crypto'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.thymeleaf.extras:thymeleaf-examspringsecurity6'

    // ✅ JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
    runtimeOnly('io.jsonwebtoken:jjwt-jackson:0.11.5') // Jackson 기반 파서
```

JwtTokenProvider 만든 이후 JwtAuthenticationFilter만들면 됨
다음으로 SecurityConfig를 stateless + JWt 필터 추가로 변경

# JWT
Spring Security는 기본적으로 세션 기반 인증을 사용하지만, REST API 서버를 개발할 때는 세션이 아니라 JWT(JSON Web Token) 기반 인증을 사용하는 경우가 많다. </br>
JWT는 유저가 로그인하면 서버가 토큰을 발급하고, 이후 모든 요청에는 이 토큰을 함께 보내서 인증을 처리하는 방식

## 1. 로그인 요청 흐름 (AuthController)
유저는 /auth/login 같은 엔드 포인트로 **ID + Password**를 보낸다 </br>
로그인 시 AuthController가 토큰 발급
```java
@PostMapping("/login")
public LoginResponse login(@RequestBody LoginRequest request) {
    // 1. 아이디로 유저 조회
    User user = userService.findByEmail(request.getEmail());

    // 2. 비밀번호 검증
    passwordEncoder.matches(request.getPassword(), user.getPassword());

    // 3. JWT 토큰 발급
    String token = jwtTokenProvider.generateToken(user);

    return new LoginResponse(token);
}
```
### ✔ 여기서 중요한 포인트
- 로그인 시 세션을 사용하지 않는다
- 인증이 성공하면 서버가 JWT 토큰을 발급한다
- 클라이언트는 이 토큰을 저장한다 (localStorage 등)

## 2. JWT 토큰 생성 담당(JwtTokenProvider)
토큰 생성 & 검증 담당 클래스 
```java
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration-seconds}")
    private long expiration;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expire)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
```
### ✔ JWTTokenProvider가 하는 일 
- 서명용 secret key 초기화
- 토큰 생성(generateToken)
- 토큰 검증(parseClaims)
- 토큰에서 사용자 정보 꺼내기 

## 3. 요청 인증 처리 (JwtAuthenticationFilter)
이 필터는 사용자가 API를 호출할 때마다 실행</br>
토큰을 읽고 인증 처리

요약 흐름 :
```java
요청 → JwtAuthenticationFilter → SecurityContext → 컨트롤러
```
핵심 코드 구조 형태 : 
```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            Claims claims = jwtTokenProvider.parseClaims(token);

            String userId = claims.getSubject();
            UserDetails userDetails = userDetailsService.loadUserById(userId);

            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
```
### ✔ JwtAuthenticationFilter 역할
- HTTP 요청 Header("Authorization")에서 JWT 파싱
- 토큰 유효성 검증
- 토큰에서 userId 꺼냄
- DB에서 userDetails 재로드
- SecurityContext에 인증 정보 저장

## 4. SecurityConfig에 필터 추가
SecurityConfig는 필터 등록 + 보호할 URL 설정 </br>
마지막 설정은 SecurityConfig에서 필터 등록 : 
```java
http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```
이렇게 설정해주면 Spring Security가 로그인 여부를 판단할 때 세션이 아니라 JWT 를 기준으로 인증하게 된다. 

## 전체 인증 흐름 
```java
[1] 로그인
Client → /auth/login(email, password)
 → 서버가 JWT 발급
 → 클라이언트가 JWT 저장

[2] API 요청
Client → Authorization: Bearer TOKEN

[3] JwtAuthenticationFilter 동작
 → 토큰 검증
 → userId 추출
 → DB에서 사용자 재조회
 → SecurityContext에 저장

[4] 인증된 요청만 컨트롤러 접근 가능
```
### 블로그 제목 
"JWT와 Spring Security로 인증 시스템 구축하기"