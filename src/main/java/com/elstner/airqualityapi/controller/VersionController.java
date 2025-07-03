package com.elstner.airqualityapi.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionController {
    @Value("${spring.application.version}")
    private  String version;

    @GetMapping("/version")
    public String getVersion() {
        // get settings from application.properties
        return version;
    }
}
