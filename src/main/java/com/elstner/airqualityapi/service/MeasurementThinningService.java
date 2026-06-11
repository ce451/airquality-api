package com.elstner.airqualityapi.service;

import com.elstner.airqualityapi.repository.MeasurementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

/**
 * Scheduled task that progressively thins out (decimates) old measurements to reduce
 * database size while keeping historical data available at a lower resolution.
 * <p>
 * Cascade (age relative to now):
 * <ul>
 *     <li>0–10 min: untouched (live, ~15s)</li>
 *     <li>10 min–1 h: one measurement per 30 s</li>
 *     <li>1 h–1 day: one measurement per 60 s</li>
 *     <li>1 day–retention: one measurement per 300 s</li>
 *     <li>&gt; retention: deleted by {@link MeasurementCleanupService}</li>
 * </ul>
 * For each band the earliest measurement per (station, time-bucket) is kept and the
 * rest deleted. The operation is idempotent: a second run within the same band deletes
 * nothing.
 * <p>
 * The API container runs in UTC and the {@code timestamp} column stores UTC-naive
 * values, so {@link ZonedDateTime#now()} yields band boundaries that match the stored
 * timestamps (same approach the read queries already rely on).
 */
@Service
public class MeasurementThinningService {

    private static final Logger log = LoggerFactory.getLogger(MeasurementThinningService.class);

    private final MeasurementRepository measurementRepository;

    @Value("${measurement.retention.days:30}")
    private int retentionDays;

    @Value("${measurement.thinning.tier1.after-minutes:10}")
    private long tier1AfterMinutes;
    @Value("${measurement.thinning.tier1.interval-seconds:30}")
    private long tier1IntervalSeconds;

    @Value("${measurement.thinning.tier2.after-minutes:60}")
    private long tier2AfterMinutes;
    @Value("${measurement.thinning.tier2.interval-seconds:60}")
    private long tier2IntervalSeconds;

    @Value("${measurement.thinning.tier3.after-minutes:1440}")
    private long tier3AfterMinutes;
    @Value("${measurement.thinning.tier3.interval-seconds:300}")
    private long tier3IntervalSeconds;

    public MeasurementThinningService(MeasurementRepository measurementRepository) {
        this.measurementRepository = measurementRepository;
    }

    /**
     * Scheduled thinning task. Default: every 5 minutes.
     */
    @Scheduled(cron = "${measurement.thinning.cron:0 */5 * * * ?}")
    public void thinMeasurements() {
        log.info("Starting scheduled measurement thinning");

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime tier1Edge = now.minusMinutes(tier1AfterMinutes);  // 10 min ago
        ZonedDateTime tier2Edge = now.minusMinutes(tier2AfterMinutes);  // 1 h ago
        ZonedDateTime tier3Edge = now.minusMinutes(tier3AfterMinutes);  // 1 day ago
        ZonedDateTime retentionFloor = now.minusDays(retentionDays);    // 30 days ago

        try {
            int t1 = measurementRepository.thinBucket(tier1IntervalSeconds, tier2Edge, tier1Edge);      // [1h, 10min) -> 30s
            int t2 = measurementRepository.thinBucket(tier2IntervalSeconds, tier3Edge, tier2Edge);      // [1d, 1h)    -> 60s
            int t3 = measurementRepository.thinBucket(tier3IntervalSeconds, retentionFloor, tier3Edge); // [retention, 1d) -> 300s

            log.info("Thinning done: removed {} ({}s band), {} ({}s band), {} ({}s band); {} total",
                    t1, tier1IntervalSeconds, t2, tier2IntervalSeconds, t3, tier3IntervalSeconds, t1 + t2 + t3);
        } catch (Exception e) {
            log.error("Error during measurement thinning", e);
        }
    }
}
