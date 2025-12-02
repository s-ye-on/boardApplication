## 패키지 구조 수정

### domain
- board.package
- post.package
- comment.package
- user.package
- 각각 패키지에 service, repository, controller 생성

### global
- exception.package
- baseEntity
- securityConfig

### request는 어디에?
- postRequest는 boardRequest를 상속받고, commentRequest는 postRequest를 상속 받고 있음
- 현재 상속 구조를 통해 관계를 표현하고 있음
- 이들을 각각의 domain에 따로 두면 안됨
- 의존 방향이 도메인 계층 의존성을 암시적으로 강제하는 꼴이 됨

### 진짜 어디에?
- dto/request 전용 상위 패키지 둠
- ADT 계층을 유지하면서 의존성 명확
- 요청 타입끼리만 관계를 맺고 도메인과 분리

### 추가 
- request는 계층 구조가 있으니 dto를 따로 둔다 했을 때
- response는 계층 구조가 없는데 어디에? 

✅ Request
•	보통 입력 모델 (Input Model), 즉 클라이언트 → 서버의 데이터 전달용.
•	계층 관계나 상속 구조를 통해
“이 요청은 어떤 상위 요청의 구체형이다”를 명확히 표현할 수 있음.
•	예: BoardRequest → PostRequest → CommentRequest
→ 도메인 간 관계나 의존성을 그대로 코드로 보여줌.

✅ Response
•	반면 Response는 출력 모델 (Output Model), 즉 서버 → 클라이언트의 데이터 표현용.
•	Response는 “읽기 전용”, “가공된 형태”, “관계 없는 projection”인 경우가 많고,
계층보단 독립적인 표현(view) 이 많아.

→ 그래서 대부분의 경우 Response는 도메인 단위로 분리하는 게 더 자연스러워.
-> Response는 각각 도메인 안에 두기 


- global.dto.request -> 인터페이스 기반, 공통 규칙 + 상속 구조
- domain 별 response -> 각 도메인에서 독립적으로 응답 DTO 관리

✅ Request를 global로 둔 이유
- 입력(request)은 Controller -> Service 방향으로 한 방향 통일된 규칙을 가져야 하고,
- 여러 도메인에서 유사한 Request 구조(예 : Create, Update)가 반복되니까
- 인터페이스 기반으로 통합해두는게 딱임

```java
public sealed interface BoardRequest permits BoardRequest.Create, BoardRequest.Update {
    record Create(String name, String description, Board.Type type) implements BoardRequest {}
    record Update(String name, String description) implements BoardRequest {}
}
```
- sealed + record DTO 패턴
- 요청의 표준화와 검증 책임 분리가 명확

### Response
```java
public record BoardReadResponse(
    Long id,
    String name,
    String description,
    Board.Type type
) {
    public static BoardReadResponse from(Board board) {
        return new BoardReadResponse(board.getId(), board.getName(), board.getDescription(), board.getType());
    }
}
```
이렇게 정적 팩토리 메서드를 두면 "Entity -> Response 변환" 로직을 엔티티 바깥에 두되
응집도는 유지할 수 있음