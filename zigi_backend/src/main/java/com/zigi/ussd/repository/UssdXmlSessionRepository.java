package com.zigi.ussd.repository;

import com.zigi.ussd.model.UssdXmlSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UssdXmlSessionRepository extends JpaRepository<UssdXmlSession, String> {
}
