package com.elstner.airqualityapi.config;

import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.repository.MeasurementRepository;
import com.elstner.airqualityapi.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SetupDatabase {
    private  static final Logger LOGGER = LoggerFactory.getLogger(SetupDatabase.class);

    @Bean
    CommandLineRunner initDatabase(StationRepository stationRepository, MeasurementRepository measurementRepository) {
        return args -> {
            var savedStation = stationRepository.save(new Station("Default Station", "127.0.0.1"));
            measurementRepository.save(new Measurement(savedStation, (short)22.0f, (short)55.3f));
            // print to console
            LOGGER.info("Preloaded Station: {}", savedStation);
            LOGGER.info("Preloaded Measurement: {}", measurementRepository.findAll().get(0));
        };
    }
}
