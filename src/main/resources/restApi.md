# 0923 스터디
## REST API

- Representational State Transfer API
- 자원을 어떻게 잘 표현할 것인가?

### URI vs URL

- URI : Uniform Resource Identifier
- URL : Uniform Resource Location

### 왜 RESTful 해야할까?

- 리모컨을 사용할 때 `+`를 눌렀을 때 보통 사람들은 `volume up` 이라고 생각함.
- 고로 누구에게나 통용될 수 있고 쉬운 통신 규약이 필요함.

### 조건

1. HTTP Method : GET, POST, PUT, PATCH, DELETE (option - HEAD) 만 사용한다.
2. 명사로만 이루어져야한다.
   - 명사로만 표현하기 힘들다면 한정적으로 동사를 사용할 수 있다. (CONTROL URI)
3. 복수형으로 작성해야 한다.
4. `/`로 계층을 구분한다.
5. 케밥체(kebap-case)를 사용한다.

#### 예시

1. 게시글 전체 가져오기
   - 👍 : `GET /posts`
   - 👎 : `GET /posts/all`
2. 1번 게시글 가져오기 : `GET /posts/1`
3. 1번 게시글 삭제하기 : `DELETE /posts/1`
4. 1번 게시글의 댓글들 가져오기 : `GET /posts/1/comments`
5. 1번 게시글의 댓글 전부 삭제하기 : `DELETE /posts/1/comments`
6. 이메일 중복 검사하기 : `POST /users/emails/validation`, `POST /users/duplicate-email`  
   - GET : 서버에 멱등성을 보장하는 자원을 조회
   - PUT : 덮어씌우기 (멱등성 X)
   - PATCH : 일부 수정 (멱등성 O)
   - DELETE : 삭제
   - POST : 생성 X. 서버에 어떤 행위를 요청함.
7. 게시글 생성하기 : `POST /posts`
8. 제목(title)이 '최'를 포함하는 게시글이 있는지 조회 : `GET /posts?title=최`
   - query string : 서버에게 질의
   - `@PathVariable` vs `@RequestParam`

### PUT vs PATCH (feat. JPA)

### @PathVariable vs @RequestParam
- ?를 쓰면 @RequestParam
- {}를 쓰면 @PathVariable

#### @PathVariable
- 리소스를 고유하게 식별할 때 사용
- URL 안에서 경로 일부가 표현되어 있는 경우
- 예시
- 특정 게시글 ID에 해당하는 댓글 목록을 가져온다
- /posts/{postId}/comments

#### @RequestParam
- 리소스 자체를 식별하는 값
- 예시
- 게시글 id, 댓글 id
- 검색 필터링 조건 -> @RequestParam
- 예시 : 작성자 이름, 정렬 기준, 페이지 번호


### @RequestParam vs @RequestBody
#### @RequestParam
- 용도 : URL 쿼리 스트링이나 폼 데이터를 읽을 때 사용
- 데이터 위치 : URL에 붙는 파라미터
- 형식 : ?key=value
- 예시
- 
  @GetMapping("/posts")
  public List<PostReadResponse> getPostsByTitle(@RequestParam String title) {
  return postService.readByTitle(title);
  }
- 호출 : GET /posts?title=String
- 특징 : 단순한 데이터(숫자, 문자열, boolean 등)에 적함
- 여러 개 받을 때는 @RequestParam List<String> tags 도 가능

#### @RequestBody
- 용도 : HTTP 요청 본문(Body)에 담긴 JSON, XML, 혹은 Form 데이터를 객체로 변환할 때 사용
- 데이터 위치 : HTTP body
- 형식 : JSON같은 구조화된 데이터
- 예시
- @PostMapping("/posts")
  public PostCreateResponse createPost(@RequestBody @Valid PostCreateRequest request) {
  return postService.createPost(request);
  }
- 호출 : POST /posts
- 요청 Body:
- {
  "title": "Spring 공부",
  "content": "REST API 배우기",
  "author": "승연"
  }
- 특징 : 복잡한 데이터 구조를 받을 때 좋음
- DTO로 바로 매핑 가능
- @Valid와 함께 사용하면 필드 검증 가능