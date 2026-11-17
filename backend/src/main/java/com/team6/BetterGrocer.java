package com.team6;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

//https://spring.io/guides/gs/spring-boot
@SpringBootApplication
@EnableCaching
public class BetterGrocer {
    public static void main(String[] args) {
        SpringApplication.run(BetterGrocer.class, args);
    }
}