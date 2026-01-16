## 페이징(Paging)
페이징(Paging)은 한 번에 전체 데이터를 다 가져오지 않고,  
필요한 일부만 나누어 조회하는 기법이다.

## 페이징이 왜 필요할까? 
페이징은(Paging)은 한 번에 전체 데이터를 조회하지 않고,  
필요한 만큼만 나누어 조회해 성능과 UX를 동시에 개선하는 기법이다.  

- 예
  - 유저가 10만명인데, /users API호출할 
    때 전부 리턴하면 DB와 네트워크가 터짐
  - 그래서 보통 `/users?page=0&size=10` 이런식으로 10명씩 끊어서 가져옴

## Spring Data JPA에서의 기본 페이징 (Offset 기반)
- Spring Data JPA에서는 Pageable과 Page를 이용해 자동으로 페이징 처리를 지원  

Spring Data JPA에서 제공하는 기본 페이징은  
SQL의 LIMIT / OFFSET을 사용하는 Offset 기반 페이징이다.   

✅ Repository
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findByNicknameContaining(String nickname, Pageable pageable);
}
```
- `Page<User>` : 결과를 페이지 단위로 감싼 객체 (총 페이지 수, 현재 페이지 등 정보 포함)

- `findByNicknameContaining` : Containing 키워드는 LIKE %keyword% SQL 구문으로 자동 변환

- `Pageable pageable` : 페이지 번호, 크기, 정렬 방식 등을 담는 객체

```sql
SELECT *
FROM user
WHERE nickname LIKE %keyword%
LIMIT ?, ?
```
- 이런 식으로 JPA가 SQL을 자동으로 만들어줌

스프링 데이터 JPA의 메서드 네이밍 규칙에 따라 
`Containing`, `StartsWith`, `EndsWith` 같은 키워드는 자동으로 JPQL로 변환

- `findByNicknameContaining("승")`
  - `WHERE nickname LIKE %승%`
- `findByNicknameStartsWith("승")`
  - `WHERE nickname LIKE 승%`
- `findByNicknameEndsWith("승")`
  - `WHERE nickname LIKE %승`

### 컨트롤러 예시 
✅ Controller
```java
@GetMapping("/users")
public Page<UserReadResponse> getUsers(
        @RequestParam(required = false) String nickname,
        @PageableDefault(size = 10, sort = "id") Pageable pageable
) {
    return userService.searchUsers(nickname, pageable);
}
```

### 서비스 예시
✅ Service
```java
public Page<UserReadResponse> searchUsers(String nickname, Pageable pageable) {
    Page<User> users = userRepository.findByNicknameContaining(nickname, pageable);
    return users.map(UserReadResponse::from);
}
```
- 여기서 `Page.map()`을 사용하면 `Page<User` -> `Page<UserReadResponse>`로 깔끔하게 변환 가능

### Page, Slice, List 차이점

| 타입         | 페이징 정보 포함 | 다음페이지 여부 | 전체 개수 조회      |
|------------|-----------|----------|---------------|
| `Page<T>`  | ✅ 있음      | ✅ 있음     | ✅ count(*) 실행 |
| `Slice<T>` | ❌ 없음      | ✅ 있음     | ❌ 빠름          |
| `List<T>`  | ❌ 없음      | ❌ 없음     | ❌ 가장 단순       |

- 관리자/검색 화면은 Page, 사용자 피드는 Slice를 많이 씀
- 단 Slice는 무한 스크롤 같은 UI에 좋음  

Page는 "페이지 이동"이 필요한 화면에,  
Slice는 "다음 데이터가 있는지만" 필요한 화면에 적합하다

### 응용 팁
- 페이징은 정렬도 함께 처리 가능함
```java
Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());
userRepository.findByNicknameContaining("승", pageable);
```
- 이렇게 하면 0페이지부터 10개씩, createdDate 기준으로 내림 차순 정렬된 결과 나옴
- 가장 최근에 생성된 데이터가 나옴(createdDate 기준 내림차순 : 최신 글-> 오래된 글)

### 정리
- 페이징은 Page + Pageable 조합으로 처리
- Containing은 자동으로 LIKE 검색 지원
- Page 객체에는 전체 페이지 수, 현재 페이지, 정렬 정보 등 다 들어 있음
- 실무에서는 Page, 무한 스크롤엔 Slice 사용
- JPA 네이밍 규칙으로 페이징 + 검색 한번에 가능


## 페이징의 두가지 개념
Offset 기반 페이징은 "페이지 번호"를 기준으로 하고,  
Cursor 기반 페이징은 "데이터의 위치(기준값)"를 기준으로 한다.  

### ✅ 1️⃣ 일반적인 페이징 (Offset 기반 Paging)
- SQL의 OFFSET과 LIMIT을 이용하는 가장 흔한 방식
```sql
SELECT * FROM post ORDER BY id DESC LIMIT 10 OFFSET 20;
```
- OFFSET 20 : 앞의 20개를 건너뛰고
- LIMIT 10 : 그 다음 10개만 가져와라
- "페이지 번호 기반" 페이징
- `PageRequest.of(page, size)`-> page는 0부터 시작

✅ 장점
- 간단하고 대부분의 DB에서 지원됨
- 전체 개수를 세기 쉬움 (-> Page 객체에서 `getTotalElements()`로 확인 가능)

❌ 단점
- 데이터가 많을 수록 느려짐
  - 예 : OFFSET 100000이면 DB는 10만개를 스캔하고 버려야 함
- 실시간 데이터(새글, 삭제 등)에는 불안정 - 페이지 밀림 현상 발생 가능
- 반드시 정렬 기준이 필요하며, 정렬이 없으면 페이지 결과가 달라질 수 있다 !

### ✅ 2️⃣ 커서 기반 페이징 (Cursor 기반 Paging)
- OFFSET 대신 "마지막으로 조회한 데이터의 기준값(cursor)"을 사용
- 예를 들어 게시글의 ID가 있다면:
```sql
SELECT * FROM post 
WHERE id < 12345 
ORDER BY id DESC 
LIMIT 10;
```
- 이렇게 "id 12345보다 작은 것 중에 10개만 가져와라"라는 방식으로 연속적인 페이지 요청을 구현

✅ 장점
- 빠르다 (OFFSET 스캔이 없음)
- 실시간 데이터에 강하다 (새글이 생겨도 페이지 밀림x)

❌ 단점
- 정렬 기준이 고정되어야 함(보통 id, createdAt 같은 단일 컬럼)
- 전체 페이지 수나 total count를 구하기 어렵다

## 스프링에서 사용하는 페이징 관련 객체
- `Pageable` : 페이징 정보를 담는 인터페이스 (page, size, sort 등)
- `PageRequest` : Pageable의 구현체, PageRequest.of(page, size, sort)로 생성
- `Page<T>` : 실제 페이징된 결과 객체. 전체 개수(`getTotalElements()`), 전체 페이지 수(`getTotalPages()`)
                현재 페이지의 내용(`getContent()`) 등을 포함
- `Slice<T>` : Page와 유사하지만 전체 개수는 포함하지 않음. (`getTotalElements()`없음). 대신 "다음 페이지가 있는가?"
               (`hasNext()`)만 알려줌
  - 오프셋 기반/ 커서 기반 모두 가능, 가벼운 조회용
  - 다음 페이지 존재하는지만 알 수 있도록 해서 page보다 가벼움
  - 하지만 사용자가 페이지 번호로 이동하려면 slice로만으로는 부족함
  - 이때는 Page를 써야함
                
- `List<T>` : 페이징 개념이 없는 단순 결과 반환용 (테스트, 소규모 데이터)

```java
// 0번 페이지, 10개씩, 생성일 기준 내림차순
Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());
```
- 컨트롤러에서 `@PageableDefault`를 사용하면 스프링이 내부적으로 `PageRequest`를 만들어서 Pageable로 넘겨줌
- 서비스에서는 `Pageable pageable`로 받으면 됨 -> 내부적으로 이미 `PageRequest` 존재

### 예시 코드 (Spring Data JPA - Offset 기반 페이징)
```java
Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());
Page<Post> page = postRepository.findAll(pageable);

System.out.println("총 게시글 수: " + page.getTotalElements());
System.out.println("전체 페이지 수: " + page.getTotalPages());
System.out.println("현재 페이지 번호: " + page.getNumber());
System.out.println("게시글 목록: " + page.getContent());
```

### 커서 기반 페이징 예시 (실무형, 효율적)
```java
@Query("SELECT p FROM Post p WHERE p.id < :lastId ORDER BY p.id DESC")
List<Post> findNextPage(@Param("lastId") Long lastId, Pageable pageable);
```
```java
// controller/service
List<Post> posts = postRepository.findNextPage(lastPostId, PageRequest.of(0, 10));
```
| 구분       | 일반 페이징 (Offset 기반)          | 커서 기반 페이징 (Cursor 기반)  |
|----------|-----------------------------|------------------------|
| 방식       | OFFSET + LIMIT 사용           | 마지막 데이터 기준값(Cursor) 사용 |
| 장점       | 간단하고 전체 개수 계산 가능            | 빠르고 실시간 데이터에 안정적       |
| 단점       | OFFSET이 클수록 느림, 밀림 현상 가능    | total count 계산 어려움     |
| 주요 용도    | 관리자 페이지, 통계 조회 등            | 무한 스크롤, 피드, 타임라인 등     |
| 스프링에서 사용 | Page, Pageable, PageRequest | Slice 또는 커서 기반 커스텀 쿼리  |


- 페이징은 controller에서 받아서 service로 전달하는게 일반적
- `@PageableDefault`, `@SortDefault`같은 기능은 controller에서만 작동
- Spring MVC는 자동으로 파싱해줌
```http request
GET /users?page=2&size=10&sort=createdDate,desc
```
```java
@GetMapping("/users")
public ResponseEntity<Page<UserResponse>> getUsers(Pageable pageable) {
    // page=2, size=10, sort=createdDate,desc
}
```
- 서비스는 단순히 Pageable 객체를 이용해 Repository만 호출하면 됨

- 예외적으로 Service 내부에서 생성하기도 함
  - 페이징 요청이 고정된 상태라면! (예 : size=10, sort=DESC)
  - 이런 경우 `PageRequest.of()`를 직접 만들어줘도 됨
    - 요청에서 받는 형태가 아니라 내부적으로 사용하는 페이징일 때만 사용됨

### Slice 예시
✅ Slice를 사용하는 CommentRepository
```java
Slice<Comment> findByPostIdOrderByCreatedDateAsc(Long postId, Pageable pageable);
```  

✅ Slice를 사용하는 CommentService
```java
public Slice<CommentReadResponse> readAllByPostId(Long postId, int size) {
		Pageable pageable = PageRequest.of(0, size); // 첫 슬라이스, size만큼
		Slice<Comment> slice = commentRepository.findByPostIdOrderByCreatedDateAsc(postId, pageable);
		return slice.map(this::mapToCommentReadResponse);
	}
```  

✅Slice를 사용하는 CommentController
```java
@GetMapping("/posts/{postId}/comments")
public Slice<CommentReadResponse> findAllByPostId(
        @PathVariable Long postId,
        @RequestParam(defaultValue = "10") int size) {  // 한번에 가져올 댓글 개수
    return commentService.readAllByPostId(postId, size);
}
```
- 댓글을 더 보려면 '더보기' 버튼을 눌러서 더 가져와야함
- 댓글이 정말 많지 않은 경우는 그냥 List로 다 보여줘도 될것이라 생각함
- Slice는 hasNext() 메서드가 있어서 다음 페이지가 있는지 쉽게 알 수 있음
- 메모리 부담이 적고, 댓글이 많아도 안정적

### 실제 내 프로젝트에서 사용 예시
✅ Controller에서의 방법 1 (BoardController)
```java
@GetMapping
public Page<BoardReadResponse> getAll() {
	Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
	return boardService.readAll(pageable);
}
```
- 수동으로 직접 Pageable 생성
- 백엔드에서 고정된 정렬 기준으로 직접 코드를 작성
- 즉 무조건 최신순 또는 이름 오름차순 같은 고정 규칙으로 강제하는 방식


✅ Controller에서의 방법 2 (BoardController)
```java
@GetMapping
	public Page<BoardReadResponse> getAll(Pageable pageable) {
		return boardService.readAll(pageable);
	}
```
- 요청 파라미터로 정렬 제어 (더 실전적 방식)
- 요청 파라미터는 프론트엔드 (API 호출자)가 작성  


✅ Controller에서의 방법 3 (PostController)
```java
@GetMapping
	public Page<PostReadResponse> getAllPosts(
		@PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC)
		Pageable pageable) {
		return postService.readALl(pageable);
	}
```
- pageable에 조건들이 자동주입되긴 하겠지만 조건 없이 요청 올 경우를 대비해 디폴트를 최신순으로 지정
- pageable 자동 주입 때문에 몇페이지 몇개 글을 가져올 지 알아서 됨
- GET /posts?page=0&size=10 이 요청이 들어오면 Spring이 자동으로 Pageable pageable = PageRequest.of(0, 10);로 만들어줌
- `@PageableDefualt`를 사용해주는 것이 안전장치 + 기본 UX를 보장할 수 있음
  - 개발자가 의도한대로 사용되게 할 수 있음  


### 참고
- 스프링 데이터 jpa에서 이미 Pageable을 인자로 받는 findAll(Pageable pageable) 버전을 자동으로 지원해줌
- findAll() -> 그냥 전부 다 List로 반환
- findAll(Pageable) -> 페이징 결과(Page)로 반환
- findAll(Sort) -> 정렬된 리스트 전체 반환 


- `@PageableDefault` : 프론트에서 page/size를 안넘길 수 있는 API에만 붙임
  -  @PageableDefault는 "기본값을 지정하기 위해 사용
  -  프론트에서 아무 파라미터를 보내지 않는다면 이렇게 기본값으로 처리할게 라는 뜻
  -  프론트에서 보낸 값이 있다면 그걸 우선으로 사용
  -  다른 곳에서는 @PageableDefault가 없는 이유
  -  대부분 항상 프론트가 Pageable을 명시적으로 전달하기 때문
  -  여긴 프론트가 파라미터를 안보낼 수 있다는걸 전제로 뒀기 때문
  -  게시판 페이지에서 "게시글 목록"을 처음 불러올 때
  -  보통 보통 /boards/{boardId}/posts 만 호출하지
  - ?page=0&size=10 이런 걸 매번 붙이지 않음

- `@PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC)`
  - createdAt은 엔티티 필드명임 그렇기에 BaseEntity에 있는 createdDate 명으로 바꿔줘야함
  - 엔티티에 해당 필드 없으면 오류
  - direction 은 정렬 방향을 의미 (ASC 오름 차순, DESC 내림 차순)

## Slice vs Cursor
둘 다 "다음 페이지가 있는지만 확인"해서 무한 스크롤에 적합하지만
차이는 "페이징 기준"이 무엇이냐에 있음

### ✅ 1️⃣ Slice — “페이지 단위 기반” 페이징
- 여전히 offset 기반 페이징
- Spring Data JPA에서 SliceRequest는 내부적으로 LIMIT ?, OFFSET ? 을 사용
- 다만 Page 처럼 count 쿼리를 날리지 않고, "다음 페이지가 있는지만" 판단하기 위해 size +1개를 조회하는 방식

- 예시 :
```java
Slice<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
```
- 요청 시 page=0&size=10
- SQL : LIMIT 11 OFFSET 0
- 결과 : 데이터 11개 중 10개만 보여주고, 11번째가 있으면 hasNext = true
- 즉, Slice는 "offset 기반의 간단한 무한 스크롤"

### ✅ 2️⃣ Cursor — “데이터 키 기반” 페이징
- offset 없이, "이전 데이터의 마지막 key(id등)를 기준으로 다음 데이터를 가져옴"
- 쿼리 예시 : 
```sql
SELECT * FROM post
WHERE id < :lastId
ORDER BY id DESC
LIMIT :size;
```
- DB가 앞에서 부터 데이터를 스캔하지 않아도 되기 때문에 훨씬 빠르고 정확하다
- Cursor 방식은 보통 대규모 데이터셋이나 실시간 피드형 앱(인스타, 유튜브...)에서 사용  

- "Slice도 무한 스크롤용이긴 한데, Cursor가 더 최적화된 버전이다"
- 댓글이나 게시판 목록처럼 "페이지 단위"로 나누는건 Slice
- 인스타처럼 계속 이어지는 피드형 구조는 Cursor  

### 📚마무리  
페이징은 단순히 데이터를 나누는 기술이 아니라,  
데이터 규모와 UX에 따라 Page, Slice, Cursor를 선택하는 설계 문제다. 
