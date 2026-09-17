package com.zigi.ussd.repository;

import com.zigi.ussd.model.UssdSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UssdSessionRepository extends JpaRepository<UssdSession, String> {
}
