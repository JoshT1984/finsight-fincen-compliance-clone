package com.skillstorm.finsight.identity_auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy
@SpringBootApplication
public class IdentityAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityAuthApplication.class, args);
    }
}
