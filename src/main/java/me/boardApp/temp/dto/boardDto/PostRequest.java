//package me.boardApp.dto.boardDto;
//
//import me.boardApp.dto.request.BoardRequest;
//
//// 게시글 요청은 결국 게시판 요청의 하위 개념임을 상속을 통해 표현
//sealed interface PostRequest extends BoardRequest
//	permits PostRequest.Create {
//	record Create(
//
//	) implements PostRequest {}
//}
