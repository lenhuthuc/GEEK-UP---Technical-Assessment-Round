package com.geekup.concert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ConcertBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConcertBookingApplication.class, args);
    }
}
