# 검증
## @Valid vs @Validated

### @Valid
valid는 Java 진영의 JSR 표준 </br>
위치 : javax.validation.Valid </br>
`MethodArgumentNotValidException` 발생 </br>
그룹검증과 메서드 검증은 지원하지 않는다 

### @Validated
Spring에서 제공 </br>
위치 : org.springframework.validation.annotation.Validated </br>
`ConstraintViolationException` 발생 </br>
그룹 검증과 메서드 검증에 사용한다 (클래스 단위 검증도 가능) </br>
그룹검증 (ex: Create/Update) 구분 </br>

### 검증
dto에 `@Notblank` 붙였다면 엔티티엔 안붙이는게 맞다
- 외부 입력값 검증은 엔티티 역할이 아님
- 엔티티는 DB 테이블과 1:1 또는 유사한 구조의 영속 모델
- 엔티티 필드 검증은 DB 제약 조건으로 충분함 (`@Column(nullable = false)`)

### 사용 예시 
DTO 정의 
```java
import jakarta.validation.constraints.*;

public class UserRequest {

    @NotBlank(groups = {Create.class, Update.class})
    private String name;

    @NotNull(groups = Update.class)
    private Long id;

    // Groups
    public interface Create {}
    public interface Update {}

    // getters, setters
}
```
예시 1 : @Valid 사용할 때 (그룹 없음)
```java
@PostMapping("/user")
public String createUser(@Valid @RequestBody UserRequest request) {
    // name만 검증됨
    return "OK";
}
```
- @Valid는 그룹이 없으니 모든 검증에서 groups=default만 적용됨
- 즉, @NotNull(groups = Update.class)같은건 작동 안하고 무시 됨

예시 2 :@Validated로 그룹 검증
Create
```java
@PostMapping("/user")
public String create(@Validated(UserRequest.Create.class) @RequestBody UserRequest request) {
    return "created";
}
```
Update
```java
@PutMapping("/user")
public String update(@Validated(UserRequest.Update.class) @RequestBody UserRequest request) {
    return "updated";
}
```
동작 차이 :

| 상황        | @valid    | @Validated             |
|-----------|-----------|------------------------|
| Create요청  | name만 검증됨 | name + (Create 그룹의 규칙) |
| Update 요청 | name만 검증됨 | name + id 필수 검증        |

### 추가 
#### 1️⃣ `@Column(nullable = false, length = 200)` vs `@Size(max = 500)`
둘은 엄연히 다르다 </br> 
`@Size` : 입력값 검증 </br>
- 적용 위치 : DTO or Entity
- 제공 : Bean Validation (서버 애플리케이션 레벨)
- 의미 
  - 자바 애플리케이션 내부에서 유효성 검증을 하는 규칙
  - "문자열 길이가 최대 500자를 넘으면 안된다"
  - DB에 접근하기 전에 먼저 검증
  - 검증 실패 -> Spring에서 400 Band Request 같은 형태로 즉시 오류 반환
`@Column` : DB 스키마 제약
- 적용 위치 : Entity
- 제공 : JPA -> DB 스키마로 반영 
- 의미 
  - DB 테이블의 컬럼 길이를 지정
  - 실제로 MySQL 테이블에 VARCHAR(200) 또는 TEXT 등으로 생성됨
  - DB에 저장할 때 길이를 초과하면
    - DB에서 오류 (Data too long for column) 발생
    - Spring은 이것을 500 Server Error 처럼 보게 됨

##### 그렇다면 어떤 것을 사용하는게 맞을까?
✅DTO 라면 : `@Size(max=???)`
- 사용자 입력 제한

✅Entity라면 : `@Column(length=??)`
- DB 컬럼 길이 설계는 `@Column(length=??)`
- 사용자가 넘긴 데이터가 그 길이를 넘어가지 않도록 DTO에서 `@Size`로 방지


