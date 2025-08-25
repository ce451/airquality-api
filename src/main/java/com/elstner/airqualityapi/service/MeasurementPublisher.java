package com.elstner.airqualityapi.service;

import com.elstner.airqualityapi.dto.MeasurementWithStationDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MeasurementPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public MeasurementPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishMeasurementUpdate(MeasurementWithStationDto measurement) {
        messagingTemplate.convertAndSend("/topic/measurements", measurement);
    }
}
