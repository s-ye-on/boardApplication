package me.boardApp.domain.user.service;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.boardApp.domain.user.dto.UserResponse;
import me.boardApp.global.response.SuccessMessage;
import me.boardApp.global.exception.CommentException;
import me.boardApp.global.dto.request.UserRequest;
import me.boardApp.global.exception.ExceptionCode;
import me.boardApp.domain.user.User;
import me.boardApp.global.exception.UserException;
import me.boardApp.domain.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	/// todo : user response 만들기
	/// 지금은 user 엔티티 자체를 반환해서 비밀번호나 개인정보가 보여질 위험이 있음
	/// 이거 사실 userDetails로 반환하면 되지 않나?

	public void join(UserRequest.Create request) {
		// 가입 이력 여부 확인
		// 중복 계정 여부 확인
		/// todo : 나중에 이 부분 커스텀 어노테이션 만들어보기
		///  @UniqueNickname만들어서 dto에 붙이면 검증 로직이 dto쪽에서 알아서 해줌
		if (userRepository.existsByNickname(request.nickName())) {
			throw new UserException(ExceptionCode.DUPLICATE_NICKNAME);
		}

		// BCryptPasswordEncoder는 매번 암호화 할때마다 다른 해시값을 만들어냄
		String encodedPassword = passwordEncoder.encode(request.password());

		User user = new User(
			request.realName(),
			request.nickName(),
			encodedPassword,
			request.email()
		);

		userRepository.save(user);
	}

	public void reJoin(UserRequest.ReJoin request) {
		User user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));

		//실명검증
		user.validateRealName(request.realName());

		boolean existNickname = userRepository.findByNickname(request.nickname()).isPresent();
		if (existNickname) {
			throw new UserException(ExceptionCode.DUPLICATE_NICKNAME);
		}

		String encodedPassword = passwordEncoder.encode(request.password());
		user.updatePassword(encodedPassword);

		user.activate(request.nickname());
	}

	/// todo : 여기는 아이디를 이메일 형식으로 만들어놨는데, 닉네임을 아이디로 쓰기로 하지 않았나? 하나로 통일해야함
	/// todo : 여기서 아이디가 틀렸을 경우와 비밀번호가 틀렸을 경우가 예외가 다르게 나가는데 이러면 보안에 취약할 거라 생각 듬
	public UserResponse.Login login(UserRequest.Login request) {
		User user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw (new UserException(ExceptionCode.INVALID_PASSWORD));
		}
		return new UserResponse.Login(user.getId(), user.getNickname(), SuccessMessage.LOGIN_SUCCESS.getMessage());
	}

	// HttpSession은 브라우저와 서버가 유지하고 있는 사용자 세션 정보
	// (로그인 시 사용자 정보나 인증 상태를 이 세션에 저장하잖아?)
	// invalidate()는 말 그대로 세션을 무효화한다는 뜻
	// -> 즉, 세션에 저장돼 있던 로그인 정보, 사용자 데이터 전부 삭제되고 사용자는 더이상 로그인된 상태가 아니게 됨
	public void logout(HttpSession session) {
		session.invalidate();
	}

	public User readByNickname(String nickname) {
		return userRepository.findByNickname(nickname)
			.orElseThrow(() -> new CommentException(ExceptionCode.NOT_FOUND_NICKNAME));
	}

//	public List<Post> readPosts(String nickName) {}
//	public List<Comment> readComments(String nickName) {}
// -> 내 생각에 게시글이나 댓글에 대한 책임은 각자의 service에서 하는게 맞는 것 같음

	public void updateNickname(UserRequest.UpdateNickname request, Long currentUserId) {
		User currentUser = userRepository.findById(currentUserId)
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));

		//현재 로그인 유저의 비밀번호 검증
		currentUser.validatePassword(request.password(), passwordEncoder);

		// 닉네임 중복 체크
		userRepository.findByNickname(request.newNickName())
			.orElseThrow(() -> new UserException(ExceptionCode.DUPLICATE_NICKNAME));

		currentUser.updateNickname(request.newNickName());
	}

	public void updatePassword(UserRequest.UpdatePassword request, Long currentUserId) {
		User currentUser = userRepository.findById(currentUserId)
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));

		currentUser.validatePassword(request.presentPassword(), passwordEncoder);

		String encodedPassword = passwordEncoder.encode(request.newPassword());
		currentUser.updatePassword(encodedPassword);
	}

	public void updateEmail(UserRequest.UpdateEmail request, Long currentUserId) {
		User user = userRepository.findById(currentUserId)
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_USER));

		user.validatePassword(request.password(), passwordEncoder);
		/// 나중에 email 중복 체크 넣어줄 수도 있음

		user.updateEmail(request.newEmail());
	}

	public void delete(UserRequest.Delete request, Long currentUserId) {
		User user = userRepository.findById(currentUserId)
			.orElseThrow(() -> new UserException(ExceptionCode.NOT_FOUND_NICKNAME));

		user.validateDelete(request, passwordEncoder);

		// soft delete
		user.inactivate();
	}

	// read 과연 read를 만들었을 때 user의 어느 부분까지 보여줘야하나?
	// 닉네임만 출력되는거 하나 만들자

	// 수정해야할 것
	// 생각해봐야할 것 user가 생겼으니 post나 comment가 작성자 정보를 다 갖고 있을 필요가 ?
	// 그냥 외래키로 작성자의 식별자만 알고 있으면 되지 않나?
	// -> post와 comment에 @ManyToOne으로 user와 연관관계 매핑
	// user쪽에 @OneToMany 필요할까? 내 생각엔 필요하다 봄 -> user를 기준으로 user가 쓴 글과 댓글에 접근해야할 상황이 생길 것 같음
	// @OneToMany 피치 못해 사용해야할 것 같은데 N+1문제에 대해 고민해봐야 함
	// delete 시 닉네임 삭제된 사용자 로 이름 변경 + 비활성화

	// 더 고민해봐야할 점 단순히 user의 상태를 inactive로 바꾸지 말고, inactive 상태가 됐을 때 닉네임을 존재하지 않는 이름 이런식으로 바꿔줘야할듯
	// + 재가입 메서드도 하나 만들면 좋을듯
}
