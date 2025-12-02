## 백엔드 개발 흐름

### 보통 Spring 애플리케이션은 3계층으로 만들어짐
- Controller : 클라이언트 요청을 받고 응답을 돌려줌 (HTTP 중심)
- Service : 비즈니스 로직(=실제 기능)을 수행
- Repository : DB와 직접 통신 (JPA, MyBatis 등)

### 정상적인 흐름(성공 케이스)
1. 클라이언트 -> POST `/boards` 요청
2. Controller에서 요청 데이터를 받음 (`@RequestBody`)
3. Service에 비즈니스 로직 수행 요청
4. Repository 통해 DB에 저장
5. Service -> Controller로 결과 반환
6. Controller가 HTTP 200(OK) 또는 201(CREATED)로 응답 반환

- [Controller] ← HTTP 201 ← [Service] ← [Repository]
- 이때는 그냥 return ResponseEntity.ok(result); 같은 식으로 보내면 끝

### 예외(오류) 흐름 (실패 케이스)
서비스 로직에서 문제가 생긴다면?
- 존재하지 않는 상품을 조회
- 유효하지 않은 비밀번호
- 재고 부족 

이런 경우에는 Service 계층에서 "예외(Exception)"을 던진다
```java
if (stock < requested) {
    throw new OutOfStockException("재고가 부족합니다.");
}
```
그럼 이 예외가 Controller로 올라오고, Spring이 `@RestControllerAdvice`에 등록된 전역 예외 처리기(ExceptionHandler)가 이걸 잡아줌

### 전역 예외 처리기 (Global Exception Handler)
"실패한 경우 ㅓ떤 로직이 실행되는지"에 대한 핵심
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ErrorResponse> handleOutOfStock(OutOfStockException e) {
        ErrorResponse body = new ErrorResponse(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
```
이렇게 하면 Service에서 예외만 던져도 알아서 HTTP 상태 코드 + 에러 메시지가 클라이언트로 전달됨
```json
{
  "message": "재고가 부족합니다."
}
```
그리고 HTTP Status는 400(BAD_REQUEST)로 나갈 것임

### 전체 흐름 요약
[Client]
↓  (HTTP 요청)
[Controller]  — 요청 DTO → Service
↓
[Service]     — 비즈니스 로직 수행
↓
[Repository]  — DB 접근

↑
[Service]  — 결과 or 예외 발생
↑
[Controller]
├─ 정상 → ResponseEntity(200 or 201)
└─ 예외 → GlobalExceptionHandler 처리 (400, 404, 500 등)