## 커스텀 어노테이션 정리

### 어노테이션이란?
어노테이션은 코드(클래스, 메서드, 필드 등)에 대한 메타데이터를 제공하는 문법.

### 1️⃣ 어노테이션 정의 : 기본구조
```java
import java.lang.annotation.*;

@Target(ElementType.METHOD)      // 어디에 붙일 수 있는지 (TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, etc.)
@Retention(RetentionPolicy.RUNTIME) // 유지 범위: SOURCE / CLASS / RUNTIME
@Documented
public @interface MyAnnotation {
    String value();            // 필수 요소
    int number() default 0;    // 기본값 있는 요소
}
```
- `@Target`: 사용할 수 있는 위치
- `@Retention` : 언제까지 어노테이션 정보가 유지되는지
  - SOURCE : 컴파일 후 제거(예 : @Override)
  - CLASS : 클래스 파일까지 남지만 런타임엔 없음
  - RUNTIME : 런타임에 리플렉션으로 읽을 수 있음
- `@Documented` : javadoc 등에 포함되도록 함
- `@Inherited` : 서브 클래스에 자동 상속되게 함(클래스 대상일 때만)
- `@Repeatable` : 같은 어노테이션을 여러 번 사용할 수 있게 함

예 : 반복 사용 가능하게 만들기
```java
@Repeatable(Roles.class)
public @interface Role {
    String value();
}

public @interface Roles {
    Role[] value();
}
```
### 자주하는 실수
- Retention을 잘못 정함 : 런타임에서 읽을 건 RUNTIME으로.(안그러면 리플렉션으로 못 읽음)
- 기본값을 제공하자: 가능한 경우 default를 써서 사용 편의성 향상
- 어노테이션에 로직 포함하지 않기 : 어노테이션은 데이터(메타데이터)만 담고, 실제 동작은 별도 프로세서나 리플렉션/AOP가 담당해야 깔끔함
- 명확한 이름 : `@SecuredForAdmin`처럼 역할이 명확한 이름 권장
- 문서화 : 어노테이션의 목적과 사용법을 자바독으로 남기기

### 고급 : 파라미터 타입으로 클래스, enum, 배열 등 사용
어노테이션의 요소 타입으로는 기본형, String, Class, enum, 다른 어노테이션, 배열을 쓸 수 있음
```java
public @interface Mapper {
    Class<?> target();
    String[] fields() default {};
    Priority priority() default Priority.NORMAL;
}

public enum Priority { LOW, NORMAL, HIGH }
```
사용 예 : 
```java
@Mapper(target = UserDto.class, fields = {"id","name"}, priority = Priority.HIGH)
public class UserMapping {}
```

### 언제 런타임/컴파일타임을 골라야 하나?
- 런타임(RetentionPolicy.RUNTIME) : 실행 중 동작 변경, AOP, DI, 리플렉션 기반 검증 등 -> 프레임워크/라이브러리와 함께
- 컴파일타임(SOURCE/Annotation Processor) : 코드 생성, 정적 검증, 빌드 시점에 처리해야 할 일 -> 어노테이션 프로세서 사용


## 용어 설명

### 1️⃣ 리플렉션(Reflection) 
= 프로그램이 자기 자신(클래스, 메서드, 필드 등)을 실행 중에 들여다 보는 기술

- 원래 자바 프로그램은 컴파일된 클래스 파일을 기반으로 동작하고, 보통은 클래스가 어떤 필드를 가지고 있는지, 어떤 메서드를 가지고 있는지 같은 정보를 개발자가 작성할 때만 알고 있다고 생각하지만?
- 리플렉션을 사용하면 프로그램이 실행되는 도중에도 :
  - "이 객체는 어떤 클래스?"
  - "이 클래스엔 어떤 메서드?"
  - "이 메서드엔 어떤 어노테이션이 붙어있나?"
  - "그 어노테이션의 값은 무엇?"
  - "이 메서드를 지금 호출?" 까지 전 부 가능
어노테이션 기반 기능들이 여기로 동작함 : 
  - 스프링의 @Controller, @Service, @Transactional, @Autowired
  - JPA의 @Entity, @Id
  - Lombok
  - Bean Validation @NotNull

### 2️⃣javadoc에 포함된다는 말이 무엇? 
javadoc = 코드 주석으로 자동 문서를 생성해주는 도구
- 자바는 문서화를 자동으로 만들어주는 툴을 제공함
- 예 :
```java
/**
 * 사용자의 이름을 반환합니다.
 */
public String getName() { ... }
```
-> 자바는 이걸 예쁜 HTML 문서로 만들어줌

✅ @Documented
- 어노테이션을 만들 떄 @Documented를 붙이면 그 어노테이션이 사용된 곳이 
javadoc 문서에 표시된다는 의미
  - 붙이지 않으면 javadoc 문서에는 그 어노테이션이 안보임 

### 3️⃣@Inherited가 서브 클래스에 적용된다?
```java
@Role("ADMIN")
public class Parent {}

public class Child extends Parent {}
```
만약 @Inherited가 없는 어노테이션이라면 :
- Parent에는 @Role("ADMIN")이 붙어 있음
- Child는 아무것도 없음(상속 안됨)

하지만 어노테이션에 @Inherited가 붙어 있다면
```java
@Inherited
public @interface Role { ... }
```
- Parent -> @Role("ADMIN")
- Child -> Parent의 어노테이션을 자동으로 상속받음 -> @Role("ADMIN")
- 즉 클래스에 붙은 어노테이션이 자식 클래스까지 유효해지는 것임
- 주의 : 메서드나 필드에는 적용되지 않고 클래스에만 의미가 있음

### 4️⃣@Repeatable이 왜 필요한가? 어노테이션 하나만 붙이면 되지 않나?
정말 필요한 케이스가 있음. 한 사용자가 여러 권한을 가질 경우?
```java
@Role("ADMIN")
@Role("USER")
@Role("MANAGER")
public class Member { }
```
이렇게 여러 개를 붙이게 만들 수 있음

스프링의 @Scheduled도 대표적인 예
```java
@Scheduled(cron = "0 0 9 * * *")
@Scheduled(cron = "0 0 18 * * *")
public void sendEmail() { }
```
### 5️⃣AnnotationRunner ? 
어노테이션 정의
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogExecution {}
```
이 메서드 실행하기전에 로그 찍자! 는 표시

어노테이션 사용
```java
public class Service {
    @LogExecution
    public void doWork() {
        System.out.println("일하는 중...");
    }
}
```
AnnotationRunner
```java
public class AnnotationRunner {
    public static void runAnnotatedMethods(Object obj) throws Exception {
        // 1. 클래스의 모든 메서드를 가져옴
        for (Method m : obj.getClass().getDeclaredMethods()) {

            // 2. 그 메서드에 @LogExecution이 붙어있는지 확인
            if (m.isAnnotationPresent(LogExecution.class)) {

                System.out.println("[LOG] 실행 전: " + m.getName());

                // 3. 메서드 실제로 실행
                m.invoke(obj);

                System.out.println("[LOG] 실행 후: " + m.getName());
            }
        }
    }
}
```
흐름
- 객체의 모든 메서드를 살펴본다
- 메서드에 `@LogExecution`이 붙어 있나?
- 붙어 있으면 : 
  - 실행 전 로그
  - 메서드 호출
  - 실행 후 로그
- 즉, 어노테이션이 붙어 있는 메서드만 골라서 특별한 동작을 해주는 구조
- 스프링 AOP나 @Transactional도 내부적으로 이 구조(리플렉션 + 프록시)와 거의 똑같음
