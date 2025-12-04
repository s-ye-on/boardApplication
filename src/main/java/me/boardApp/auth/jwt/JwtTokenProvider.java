package me.boardApp.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

// spring bean 으로 등록해서 어디서든 DI 받아서 쓸 수 있도록 만들어 둔 것
@Component
public class JwtTokenProvider {

	// @Value : application.yml에 있는 설정 값을 읽어오고 있음
	@Value("${app.jwt.secret}")
	private String secretKeyPlain;

	@Value("${app.jwt.expiration-seconds}")
	private long tokenValidityInSeconds;

	private Key key;

	// 서버가 시작될 때 init()가 실행되어 JWT 서명에 필요한 Key 객체를 미리 생성해둠
	// key = Keys.hmacShaKeyFor(secretKeyPlain.getBytes()); : secret 문자열을 HMAC-SHA25에 맞는 key로 변환
	@PostConstruct
	public void init() {
		// HS256 용 비밀 키 생성 (secret 문자열을 기반으로)
		this.key = Keys.hmacShaKeyFor(secretKeyPlain.getBytes());
	}

	// JWt를 생성하는 메서드
	public String generateAccessToken(Long userId, String email, String role){
		long now = System.currentTimeMillis();
		Date validity = new Date(now + tokenValidityInSeconds * 1000);

		return Jwts.builder()
			.setSubject(String.valueOf(userId))		// sub : 유저 ID
			.claim("email", email)
			.claim("role", role)
			.setIssuedAt(new Date(now))
			.setExpiration(validity)
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	public boolean validateToken(String token){
		try{
			Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token);
			return true;
		} catch(SecurityException | MalformedJwtException e){
			// 잘못된 JWT 서명
			return false;
		} catch(ExpiredJwtException e){
			//토큰 만료
			return false;
		} catch (UnsupportedJwtException e){
			return false;
		} catch(IllegalArgumentException e){
			return false;
		}
	}

	public Long getUserId(String token){
		Claims claims = parseClaims(token);
		return Long.valueOf(claims.getSubject());
	}

	public String getEmail(String token){
		Claims claims = parseClaims(token);
		return claims.get("email", String.class);
	}

	public String getRole(String token){
		return parseClaims(token).get("role", String.class);
	}

	private Claims parseClaims(String token){
		return Jwts.parserBuilder()
			.setSigningKey(key)
			.build()
			.parseClaimsJws(token)
			.getBody();
	}
}
