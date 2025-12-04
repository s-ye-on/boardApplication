//package me.boardApp.global;
//
//import me.boardApp.domain.comment.Comment;
//import me.boardApp.domain.notice.Notice;
//import me.boardApp.domain.post.Post;
//import me.boardApp.domain.user.User;
//import me.boardApp.global.exception.ExceptionCode;
//import me.boardApp.global.exception.NoticeException;
//import me.boardApp.global.exception.PostException;
//import me.boardApp.global.exception.UserException;
//import org.springframework.stereotype.Service;
//
//
//public class AuthorizationService {
//	// 권한 검증(Authorization)이 필요한 이유
//	// 엔티티에 작성된 user와 현재 로그인한 user(=currentUser)가 동일인인지 비교해야함
//
//	// 기존 로직이 인증(Authentication)이라면,
//	// 닉네임 & 비밀번호 기반 본인 확인
//	// 말 그대로 너 A맞지?
//
//	// 권한 로직(Authorization)은
//	// 현재 로그인 주체가 글을 수정할 권한이 있는가?
//	// 작성자 본인인가? or 관리자(admin)인가?
//
//	// Board 관련
//
//
//	// Post 관련
//	public void canPostUpdate(Post post, User currentUser) {
//		if(post instanceof Notice && currentUser.isAdmin()){
//			throw new NoticeException(ExceptionCode.FORBIDDEN_ADMIN);
//		}
//
//		if(!post.isWriter(currentUser) && currentUser.isAdmin()){
//			throw new PostException(ExceptionCode.FORBIDDEN_POST_UPDATE);
//		}
//	}
//
//	public void canPostDelete(Post post, User currentUser) {
//		if(!post.isWriter(currentUser) && currentUser.isAdmin()){
//			throw new PostException(ExceptionCode.FORBIDDEN_POST_DELETE);
//		}
//	}
//
//	// Comment 관련
//	public void canDeleteComment(Comment comment, User user){
//		validateAdmin(user);
//	}
//
//	// 내부에서만 사용하는 메서드
//	private void validateWriter(User writer, User user){
//		if(!writer.equals(user)){
//			throw new UserException(ExceptionCode.FORBIDDEN_POST_ACCESS);
//		}
//	}
//
//	private void validateAdmin(User user) {
//		if(user.isAdmin()){
//			throw new UserException(ExceptionCode.FORBIDDEN_ADMIN);
//		}
//	}
//}
