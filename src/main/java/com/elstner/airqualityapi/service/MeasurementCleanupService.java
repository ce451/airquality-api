package com.elstner.airqualityapi.service;

import com.elstner.airqualityapi.repository.MeasurementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class MeasurementCleanupService {

    private static final Logger log = LoggerFactory.getLogger(MeasurementCleanupService.class);

    private final MeasurementRepository measurementRepository;

    @Value("${measurement.retention.days:30}")
    private int retentionDays;

    public MeasurementCleanupService(MeasurementRepository measurementRepository) {
        this.measurementRepository = measurementRepository;
    }

    /**
     * Scheduled task that runs daily at 2:00 AM to delete old measurements.
     * Removes measurements older than the configured retention period (default: 30 days).
     * <p>
     * Cron expression: "0 0 2 * * ?" means:
     * - 0 seconds
     * - 0 minutes
     * - 2 hours (2 AM)
     * - every day of month
     * - every month
     * - any day of week
     */
    @Scheduled(cron = "${measurement.cleanup.cron:0 0 2 * * ?}")
    public void cleanupOldMeasurements() {
        log.info("Starting scheduled cleanup of measurements older than {} days", retentionDays);

        ZonedDateTime cutoffDate = ZonedDateTime.now().minusDays(retentionDays);
        log.debug("Cutoff date for deletion: {}", cutoffDate);

        try {
            int deletedCount = measurementRepository.deleteByTimestampBefore(cutoffDate);
            log.info("Successfully deleted {} old measurements", deletedCount);
        } catch (Exception e) {
            log.error("Error during measurement cleanup", e);
        }
    }

    /**
     * Manual cleanup trigger - can be called via API endpoint if needed
     * @param daysToRetain Number of days to retain
     * @return Number of deleted records
     */
    public int manualCleanup(int daysToRetain) {
        log.info("Manual cleanup triggered with retention period of {} days", daysToRetain);
        ZonedDateTime cutoffDate = ZonedDateTime.now().minusDays(daysToRetain);

        try {
            int deletedCount = measurementRepository.deleteByTimestampBefore(cutoffDate);
            log.info("Manual cleanup deleted {} measurements", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("Error during manual cleanup", e);
            throw e;
        }
    }
}
