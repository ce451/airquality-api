package com.elstner.airqualityapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SetupDatabase {
    private  static final Logger LOGGER = LoggerFactory.getLogger(SetupDatabase.class);

    CommandLineRunner initDatabase() throws Exception {
        return args -> {

        };
    }
}
