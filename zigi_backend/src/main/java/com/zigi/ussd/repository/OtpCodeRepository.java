package com.zigi.ussd.repository;

import com.zigi.ussd.model.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    List<OtpCode> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);
}
