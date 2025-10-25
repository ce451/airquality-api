package com.elstner.airqualityapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AirQualityApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AirQualityApiApplication.class, args);
    }

}
