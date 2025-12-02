## PRG 패턴
PRG(Post -> Redirect -> Get) 패턴은 
폼 전송 같은 POST 요청 이후 새로고침 시 중복 요청이 생기는걸 막기 위해 쓰이는 패턴

- 사용자가 게시글 작성(POST/boards)후 새로 고침하면 같은 요청이 다시 실행돼서 게시글이 중복 등록될 수 있음
- 그래서 POST 이후 바로 응답을 주는게 아니라,
리다이렉트(GET/boards/{id})로 보내서 새로 고침 시 GET만 다시 일어 나게 만드는 방식

예시:
```java
@PostMapping
public String create(@ModelAttribute BoardRequest.Create request) {
    boardService.create(request);
    return "redirect:/boards"; // ✅ PRG 적용
}
```
- API 기반으로 (JSON 요청/응답) 개발하고 있다면 사실 PRG는 필요 없음
- PRG는 HTML form 기반 애플리케이션에서 주로 쓰는 패턴
- 나처럼 RESTful API로 JSOn을 주고받는 구조라면 "PRG는 사용하지 않았다" 라고 보면 됨