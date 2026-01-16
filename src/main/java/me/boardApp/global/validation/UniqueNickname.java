package me.boardApp.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// @UniqueNickname은 Bean Validation 기반의 커스텀 검증 어노테이션
// @Valid 사용 시 자동 검증
// 서비스 레벨의 로직 없이, Request DTO 수준에서 막을 수 있음
@Target({ElementType.FIELD}) // 필드에만 붙일 수 있음
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueNicknameValidator.class) // 검사할 클래스 지정
public @interface UniqueNickname {

	String message() default "중복된 닉네임 입니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
