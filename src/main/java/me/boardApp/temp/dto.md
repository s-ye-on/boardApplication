## Facade

- 외벽
- 외부에 간단하게 보여주는 디자인 패턴
- 스프링 개발자들은 너무 쉽게 이해하는 패턴 (`@Service`)

### Helper method

- 코드를 더 쉽게 이해할 수 있도록 분리된 메서드

### 순환 참조 (dependency cycle)



### CommentRequest
- 나중에 댓글이 유저 프로필에 댓글을 단다거나, 이벤트 댓글이 있거나 할 때는 그냥 독립적인 CommentRequest로 두는 것이 좋음
- 댓글이 다른 컨텍스트에서 필요하다는 요구 사항이 생기면 그때 CommentRequest를 독립
- YAGNI

### sealed interface CommentRequest
- permits 뒤에 지정한 record들만 상속 가능하게 제한함
- 즉, 다른 누군가가 몰래 새로운 타입의 요청을 추가할 수 없음


- record
  - DTO의 핵심 특징인 불변성(immutable)을 자동으로 제공
  - getter, equals, hashCode, toString 도 자동 생성돼서 깔끔함
- Create, Update, Delete
  - 각각의 요청을 명확히 구분 가능
  - 예를 들어, 서비스 레이어에서 instanceof로 타입을 구분하거나 sealed switch 문을 통해 분기 처리 가능
- 서비스 계층에서 사용하는 예시
```java
@Service
public class CommentService {

    public void handle(CommentRequest request) {
        switch (request) {
            case CommentRequest.Create create -> handleCreate(create);
            case CommentRequest.Update update -> handleUpdate(update);
            case CommentRequest.Delete delete -> handleDelete(delete);
        }
    }

    private void handleCreate(CommentRequest.Create req) {
        System.out.println("댓글 생성: " + req.content());
    }

    private void handleUpdate(CommentRequest.Update req) {
        System.out.println("댓글 수정: " + req.id());
    }

    private void handleDelete(CommentRequest.Delete req) {
        System.out.println("댓글 삭제: " + req.id());
    }
}
```
이렇게 쓰면 좋은 점
1. 타입 안정성 - 요청 종류가 컴파일 타임에 명확히 구분됨
2. 확장 제한 - 불필요한 DTO 추가를 막을 수 있음
3. 가독성 향상 - "요청 타입별"로 구조가 깔끔하게 정리됨
4. 유지보수 용이 - 나중에 새로운 요청 타입을 추가하려면 permits 에 명시해야 해서 실수로 안 넣는 걸 방지해줌

### 🧩 1️⃣ 왜 이렇게 상속 구조로 만들었는가?
우리가 sealed interface + record 구조로 만들었었음

근데 PostRequest가 BoardRequest를 상속 받고, CommentRequest가 PostRequest를 상속 받음

우리가 sealed interface + record 구조로 만든 이유는 크게 두가지 
1. ADT(Algebraic Data Type) - 명확한 요청 타입 계층 표현
    - 상속으로 표현해서 게시판 -> 게시글 -> 댓글 이라는 도메인 계층 관계를 그대로 반영
    - 게시글은 특정 게시판이 존재해야만 의미가 있고, 댓글은 특정 게시글이 존재해야 의미가 있음
    - 그 구조적 의존성을 "코드 수준에서 명확히 보이도록 한 것"

2. Validation & 타입 안정성의 확장성
    - 예를 들어 PostRequest가 BoardRequest를 상속하면, PostRequest가 사용하는 서비스나 validator에서 BoardRequest의 공통 제약을 재사용할 수 있음
    - 또 sealed interface 덕분에 컴파일러가 "허용된 Request 타입" 만 받아들이기 때문에 API 설계나 테스트에서도 타입 안정성이 강화됨

### 추가
Algebraic Data Type(ADT)
- 한국어로는 대수적 데이터 타입이라 부름
- 함수형 프로그래밍 언어에서 자주 등장하는 개념
- "여러 타입을 조합해서 새로운 타입을 정의하는 방식"

두가지 기본 형태가 존재
- Sum Type
- Product Type

Product Type(곱 타입)
- 여러 값을 묶어서 하나의 타입으로 만드는 것
- Java의 class나 record, C의 struct가 여기에 해당
- 이름이 "곱"인 이유는 경우의 수를 곱하기 때문

```java
record Point(int x, int y) {}
```
- Point는 x와 y 두 값을 "곱"한 타입
- 예를 들어 x가 10가지, y가 20가지 값을 가질 수 있다면 Point는 200가지 조합이 존재

Sum Type (합 타입)
- 여러 타입 중 하나만 선택할 수 있는 타입
- Java로는 sealed interface + record 조합이 이 개념과 거의 동일

```java
sealed interface Shape permits Circle, Rectangle {}

record Circle(double radius) implements Shape {}
record Rectangle(double width, double height) implements Shape {}
```
- Shape는 Circle 또는 Rectangle일 수 있음
- 즉, Shape는 타입의 합(Sum)이다

정리

| 구분           | 개념             | Java 예시                                          | 의미     |
|--------------|----------------|--------------------------------------------------|--------|
| Product Type | 여러 필드를 묶은 타입   | `record Point(int x, int y)`                     | AND 관계 |
| Sum Type     | 여러 타입 중 하나를 선택 | sealed interface Shape permits Circle, Rectangle | OR 관계  |