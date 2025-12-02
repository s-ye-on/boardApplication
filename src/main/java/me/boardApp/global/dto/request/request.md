## nested class를 활용한 DTO 관리
### 1.  (sealed) interface + record
```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import me.boardApp.domain.board.Board;

public sealed interface BoardRequest
	permits BoardRequest.Create, BoardRequest.Update,
	PostRequest {
	record Create(
		@NotBlank
		String name,

		@NotBlank
		String description,

		@NotBlank
		Board.Type type
	) implements BoardRequest {
	}

	record Update(
		@NotBlank
		@Size(min = 1, max = 50)
		String name,

		@NotBlank
		@Size(min = 1, max = 50)
		String description
	) implements BoardRequest {
	}
}
```

- enum class란? enumerate(열거하다) 열거형
- enum의 재밌는 속성 -> singleton (바뀌지 않음. 애플리케이션이 종료되는 순간까지 모든 instance(메모리)가 같음)
  - singleton : "프로그램 전체에서 단 하나의 인스턴스만 존재하도록 보장하는 패턴"
  - 한 클래스가 여러 번 new 로 만들어져도 실제로는 하나의 객체만 존재하게 하는 구조
  - enum이 singleton인 이유 ? 
    - enum은 자바에서 JVM이 클래스를 로드할 때 한 번만 생성하고, 모든 곳에서 동일한 인스턴스를 공유함
    - enum은 자바에서 가장 완벽하고 안전한 싱글턴 구현 방식
- enum 간의 비교 -> Object.equals(), '==' (O)
- `==` 가 정석
    - 추가 정보
      - `==` 는 주소(참조) 비교
      - 단 기본형일 경우엔 `==`는 값 그 자체를 비교함
    - `.equals()`
      - 값(내용) 비교
      - 내부 데이터가 같은지 확인
- `enum` 은 내부적으로 싱글톤 객체라서, `==`로 비교해도 주소 = 값 이 동일하게 취급됨 
- -> 그렇기에 `==`를 쓰는게 일반적임

 
- sealed의 핵심은 class의 enum형
- 상속의 특징 -> 부모는 어떤 녀석이 자신을 상속하고 있는지 모름 (자식의 존재를 모른다)
- 부모가 자식의 존재들을 모두 앎. 제약조건 -> 같은 package 상에 위치해야 한다.
 
ADT(추상 데이터 클래스) ✅



 2. class + record
```java
public final class BoardRequest {
	public record Create(

	) {
	}
}
```

 3. class + class

 4. (sealed) interface + class
```java
public interface BoardRequest {
	@Data
	class Create {
		@NotBlank
		String name;

		@NotBlank
		String description;

		Board.Type type;
	}
}
```
### PostRequest
- adt의 성질을 이용하여 이 dto는 특정 요청/응의 유형이다~ 라는걸 가시적으로 보여주기 위해 extends boardRequest해줌
- 여기서는 뭔가 추가적인 관계가 있는 경우를 말한거긴 함
- board와 post의 밀접한 관계성이 있는 경우 추가적인 상속을 해서 관계를 표현해줄 수 있는거
- 추가적인 관계 board가 존재해야 post가 존재할 수 있는 그런 조건을 말함
- 게시글 요청은 결국 게시판 요청의 하위 개념임을 상속을 통해 표현

### CommentRequest
- 나중에 댓글이 유저 프로필에 댓글을 단다거나, 이벤트 댓글이 있거나 할 때는 그냥 독립적인 CommentRequest로 두는 것이 좋음
- 댓글이 다른 컨텍스트에서 필요하다는 요구 사항이 생기면 그때 CommentRequest를 독립
- YAGNI

### Update
```java
package me.boardApp.global.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public sealed interface Update extends UserRequest
	permits Update.Nickname, Update.Password {
	record Nickname(
		@NotBlank
		String presentNickname,

		@NotBlank
		@Size(min = 3, max = 30, message = "닉네임은 최소 3자, 최대 30자까지 가능합니다")
		String newNickName,

		@NotBlank
		String password
		)implements Update{}

	record Password(
		@NotBlank
		String nickname,

		@NotBlank
		String presentPassword,

		@NotBlank
		String newPassword
	) implements Update{}
}
```

- 이런식으로 하면 UserRequest.Update.Nickname
- 이런식으로도 둘 수 있음 -> 좀 더 깔끔하긴 함
- 일단 현재 이건 사용하진 않음 이렇게하면 더 깔끔하게 갈 수도 있다 하는걸 보여주기 위해 존재