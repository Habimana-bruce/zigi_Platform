package com.zigi.ussd.repository;

import com.zigi.ussd.model.ClientSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientSessionRepository extends JpaRepository<ClientSession, Long> {

    Optional<ClientSession> findByTokenAndEndedAtIsNull(String token);

    List<ClientSession> findByChannel(String channel);
}
