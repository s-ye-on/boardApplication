# Bulk Update
bulk update라 함은 @Query + @Modifying을 같이 쓰는게 bulk update?  
👉대부분 그렇다 (Spring Data JPA 기준)

## Bulk Update 정의
영속성 컨텍스트를 거치지 않고, DB에 직접 UPDATE/DELETE를 날리는 쿼리 

대표적인 형태:
```java
@Modifying
@Query("update User u set u.nickname = :nick where u.id = :id")
void updateNickname(...);
```
또는 
```java
@Modifying
@Query("delete from Post p where p.createdDate < :time")
void deleteOldPosts(...);
```
이런 쿼리들은 : 
- ❌엔티티 로딩 안 함
- ❌Dirty Checking 안함
- ❌1차 캐시 (영속성 컨텍스트) 무시 
- ✅DB에 바로 SQL 실행  

그래서 bulk update/ bulk delete라고 부른다 

---

## 2. 왜 bulk update가 위험한가? 
핵심 문제는 **영속성 컨텍스트와 DB 상태 불일치**  

예를 들어: 
```java
Post post = postRepository.findById(1L).get();  // 영속 상태
postRepository.migrate(...);                  // bulk update 실행
```
DB에서는 board_id가 바뀌었지만: 
```text
영속성 컨텍스트 안의 post.board는 여전히 예전 board
```
즉:
- ❗️메모리 상태 != DB 상태  

이 상태로 로직을 계속 돌리면:
- 잘못된 데이터 기반 로직 실행
- 나중에 flush 시 덮어쓰기 위험
- 디버깅 지옥  

그래서 bulk update 이후에는 
```java
entityManager.clear();
```
로 **영속성 컨텍스트를 날려서 DB 기준으로 다시 읽게 만들어야 한다**

---
## 현재 내 코드에서의 PostRepository.mirage는 Bulk Update가 맞다
```java
@Modifying
@Query("update Post p set p.board =:newBoard where p.board =:oldBoard")
void migrate(...)
```
정확히 bulk update
- JPQL update
- @Modifying 사용
- 엔티티 로딩 없음
- DB 직접 반영 

---

## 4. 그럼 지금 BoardSerivce.delete()에서 em.clear()가 필요할까? 
```java
Board target = getById(id);                 // 영속 상태
Board tempBoard = boardRepository.findByType(...); // 영속 상태

postService.migrate(target, tempBoard);    // bulk update 실행

boardRepository.delete(target);            // delete 실행
```
### 핵심 판단
#### ✅Case 1 - 현재 코드에서는 문제 가능성 있음
왜나면 : 
1. target Board 엔티티는 이미 영속 상태
2. 그 Board를 참조하던 Post들이 DB에서 bulk update로 다른 board로 이동 됨
3. 하지만 영속성 컨텍스트 안에는:
   - Post 엔티티가 로딩돼 있었다면 ❌stale 상태
   - Board.posts 컬렉션이 로딩돼 있었다면 ❌ stale 상태  

예를 들면 이런 상황이 가능함:
```java
target.getPosts()   // 여전히 이전 post 목록을 들고 있음
```
이 상태에서:
- cascade
- orphanRemoval
- 추가 로직
- flush 타이밍  
이 얽히면 아주 미묘한 버그가 터질 수 있음

---
#### Case 2 - 지금은 안전해 보이는 이유 
현재 코드에서는:
- migrate 직후 
- Post 엔티티를 다시 접근하지 않고
- 바로 boardRepository.delete(target)만 호출
- target.getPosts()를 사용하지 않음  

즉 :
- 지금 코드 흐름만 보면 즉시 터질 가능성은 낮다  

⚠️하지만 유지보수 중 누군가 target.getPosts()를 다시 쓰기 시작하면 바로 위험해진다

---
## 실무적으로 추천하는 정답 
### 🎯 가장 안전한 패턴
bulk update 직후 명시적으로 clear 한다 
```java
@Transactional
public void migrate(Board fromBoard, Board toBoard) {
    postRepository.migrate(fromBoard, toBoard);
    entityManager.clear();
}
```
또는 Repository쪽에서:
```java
@Modifying(clearAutomatically = true)
@Query("update Post p set p.board =:newBoard where p.board =:oldBoard")
void migrate(...);
```
Spring Data JPA가 자동으로 clear 해준다

--- 
## clearAutomatically 옵션 추천
```java
@Modifying(clearAutomatically = true)
```
이 옵션을 붙이면:
- bulk update 실행 직후
- 영속성 컨텍스트 자동 clear

그래서 서비스 코드에서 실수할 여지가 줄어든다

PostRepository.migrate 수정
```java
@Modifying(clearAutomatically = true)
@Query("update Post p set p.board = :newBoard where p.board = :oldBoard")
void migrate(@Param("oldBoard") Board oldBoard,
             @Param("newBoard") Board newBoard);
```

