# SuccessMessage가 여기 있는 이유

## global/dto/response 밑이 아니라 global/response인 이유
- 메시지 enum은 DTO가 아님
- record DTO와 성격이 다름
- 실제로 메시지는 "응답 모델이 아니라 응답 규칙/정책"
- -> SuccessMessage는 global안에 두고, global/dto 안에 response 패키지 안에 실제 응답 DTO들(record 등)

## Record vs Enum
- Enum은 고정된 상수 집합
- Record는 다양한 데이터 속성을 가진 객체



### Enum
- Enum 클래스의 생성자는 private으로 외부에서 값을 설정할 수 없다
- 내가 직접 private을 쓰지 않아도 컴파일러가 자동으로 private으로 만든다
- `@RequiredArgsConstructor`를 enum에 붙이면 필드 기반으로 생성자를 만들어주는데, 그 생성자도 자동으로 private이 된다

```java
@RequiredArgsConstructor
public enum ExceptionCode {
    INVALID_REQUEST("잘못된 요청"), // ...
    SERVER_ERROR("서버 에러");

    private final String message;
}
```
위 코드는 컴파일되면 내부적으로 이런 형태가 된다
```java
public enum ExceptionCode {
    INVALID_REQUEST("잘못된 요청"),
    SERVER_ERROR("서버 에러");

    private final String message;

    // 롬복이 생성하지만 enum이라 자동으로 private 처리됨
    private ExceptionCode(String message) {  
        this.message = message;
    }
}
```
- enum 생성자는 왜 private이어야 할까?
- enum은 "상수 집합" -> 외부에서 마음대로 생성되면 안 되는 타입
- enum 생성자는 컴파일러가 강제로 private으로 고정. public, protected 사용 시 컴파일 오류
