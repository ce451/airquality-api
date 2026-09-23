package com.elstner.airqualityapi.service;

import com.elstner.airqualityapi.dto.MeasurementWithStationDto;
import com.elstner.airqualityapi.model.Measurement;
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

    public void publishMeasurementUpdate(Measurement measurement) {
        MeasurementWithStationDto webSocketUpdate = new MeasurementWithStationDto();
        webSocketUpdate.setId(measurement.getId());
        webSocketUpdate.setStationId(measurement.getStation().getId());
        webSocketUpdate.setTemperature(measurement.getTemperature());
        webSocketUpdate.setHumidity(measurement.getHumidity());
        webSocketUpdate.setAbsoluteHumidity(measurement.getAbsoluteHumidity());
        webSocketUpdate.setVoltage(measurement.getVoltage());
        webSocketUpdate.setTimestamp(measurement.getTimestamp());
        publishMeasurementUpdate(webSocketUpdate);
    }
}
