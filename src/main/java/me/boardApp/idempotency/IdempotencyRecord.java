package me.boardApp.idempotency;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class IdempotencyRecord {
	@Id
	private String requestId; // 클라이언트가 전달한 Idempotency-Key
	private String status; // COMPLETED PROCESSING 등
	private Integer statusCode;
	@Lob
	private String responseBody;
	private LocalDateTime createdAt;
}
