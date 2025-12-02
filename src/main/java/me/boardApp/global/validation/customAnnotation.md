## 커스텀 어노테이션

### 어노테이션 정의
```java
package me.boardApp.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD}) // 필드에만 붙일 수 있음
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueNicknameValidator.class) // 검사할 클래스 지정
public @interface UniqueNickname {

    String message() default "이미 사용 중인 닉네임이에요.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```
- `@Target({ElementType.FIELD})`
  - 이 어노테이션 필드(멤버 변수)에만 붙일 수 있도록 지정
  - 예 : DTO의 nickname 필드 위에 `@UniqueNickname` 이렇게 씀


- `@Retention(RetentionPolicy.RUNTIME)`
  - 이 어노테이션 정보를 런타임에도 유지하라는 의미. 런타임 검증(Validator)이 이 정보를 읽어야 해서 필수


- `@Constraint(validatedBy = UniqueNicknameValidator.class)`
  - 이 어노테이션이 붙은 필드를 검증할 검증기(Validator) 클래스를 지정
  - 여기선 `UniqueNicknameValidator`가 실제 검사 로직을 담당하겠다는 뜻


- `public @interface UniqueNickname {...}`
  - 새로운 어노테이션 타입을 선언하는 문법
  - `@interface`로 선언하면 `@UniqueNickname`처럼 사용할 수 있음


- `String message() default "이미 사용 중인 닉네임입니다";`
  - 검증 실패 시 기본으로 보여줄 에러 메시지를 정의함
  - 필요하면 이 값을 어노테이션 사용할 때 덮어쓸 수도 있음


- `Class<?>[] groups() default {};`
  - 검증 그룹 기능을 지원하기 위한 표준 필드. 보통은 그대로 두고 씀


- `Class<? extends Payload>[] payload() default {};`
  - 추가 메타데이터 전달용인데, 거의 안쓰임. 표준 시그니처로 항상 포함해줌


### 검증 로직 작성 (Validator 클래스)

```java
package me.boardApp.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import me.boardApp.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UniqueNicknameValidator implements ConstraintValidator<UniqueNickname, String> {

    private final UserRepository userRepository;

    @Override
    public boolean isValid(String nickname, ConstraintValidatorContext context) {
        if (nickname == null || nickname.isBlank()) {
            return true; // 다른 @NotBlank 등이 처리하니까 여기선 통과시킴
        }
        return !userRepository.existsByNickname(nickname);
    }
}
```
- `@Component`
  - 이 Validator를 스프링 빈으로 만들어줌 그래야 UserRepository같은 다른 빈을 주입받을 수 있음
  - (BeanValidation 표준처럼 스프링이 ConstraintValidator를 관리하려면 스프링 빈으로 등록해주는게 편함)


- `RequiredArgsConstructor` (롬복)
  - final로 선언된 필드에 대한 생성자 자동으로 만들어줌 
  - 덕분에 `userRepository`를 생성자 주입으로 편하게 받을 수 있음


- `public class UniqueNicknameValidator implements ConstraintValidator <UniqueNickname, String.`
  - `ConstraintValidator<A, T>`를 구현하는 클래스
  - A는 어노테이션 타입 (UniqueNickname)
  - T는 검증 대상 타입(String)
  - 즉, `@UniqueNickname`가 붙은 String 필드를 검사


- `private final UserRepository userRepository;`
  - DB 조회를 위해 UserRepository를 주입 받음
  - `existsByNickname(...)` 같은 메서드로 중복 여부를 확인


- `@Override public boolean isValid(String nickname, ConstraintValidatorContext context)`
  - 핵심 검사 메서드
  - 반환값 true면 검증 통과
  - false면 검증 실패 (폼 바인딩 시 에러가 발생)


- `if(nickname == null || nickname.isBlank()) {return true;}`
  - 주의할점 : 여기서 null/빈값을 true로 처리한 이유는 `@NotBlank` 같은 
  다른 표준 검증 어노테이션이 따로 책임을 갖기 위해서임
  - 즉 null 또는 빈 문자열 자체 검증은 별도 어노테이션에 맡기자는 관용적 패턴


- `return !userREpository.existsByNickname(nickname);`
  - 핵심 로직. DB에 같은 닉네임이 있으면 `existsByNickname`은 true를 반환하니까
    앞에 !를 붙여서 DB에 없을때만 true 리턴하도록 했음
  - 즉, 닉네임이 이미 존재하면 false(검증 실패) 하는 셈임


### Request DTO에서 사용 
```java
package me.boardApp.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import me.boardApp.global.validation.UniqueNickname;

public record UserSignUpRequest(
    @NotBlank
    @UniqueNickname // ✅ 요 한 줄이 바로 중복 검사 수행
    String nickname,

    @NotBlank
    String password
) {}
```
### 동작 흐름(요약)

1. Controller에서 `@Valid`로 DTO를 받으면, 스픵이 Bean Validation을 실행
2. DTO 필드에 `@UniqueNickname`이 붙어 있으면,
`UniqueNicknameValidator.isValid(...)`가 호출됨
3. `isValid`는 `UserRepository`로 DB 조회를 해서 중복 여부를 판단
4. `isValid`가 false면 `MethodArgumentNotValidException`이 발생
-> 400 Request로 돌아감
   (예외 핸들러를 만들어서 에러 메시지를 예쁘게 반환하면 좋음)

### 주의 사항
- DB 유니크 제약은 꼭 함께 설정
  - Validator는 편리하면 race condition(동시성) 상황에서 100% 안전하지 않음
  - 따라서 엔티티에 `@Column(unique=true)` 또는 DB 스키마에서 UNIQUE 제약을 꼭 걸어주자


- Validator에서 userRepository 같은 DB 접근을 하는 건 보통 괜찮지만, 검증 시 성능에 신경 쓰자
  - 너무 많은 검증 DB 호출은 요청 처리 비용을 올릴 수 있음


- `@Component`로 등록하지 않고도 Validator를 등록하는 방법(스프링 설정을 통해)도 있지만,
    `@Component`가 가장 간편함
