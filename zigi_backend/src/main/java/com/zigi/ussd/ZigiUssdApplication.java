package com.zigi.ussd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ZigiUssdApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZigiUssdApplication.class, args);
        System.out.println("Zigi USSD backend running at http://localhost:6157/api/customers");
    }
}
