package me.boardApp.log;

import org.springframework.data.jpa.repository.JpaRepository;


public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
}
