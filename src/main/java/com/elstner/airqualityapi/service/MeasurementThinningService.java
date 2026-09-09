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
     * Tier 1 ([1h, 10min) -> 30s). Default: every 5 minutes.
     * <p>
     * The tiers run on separate schedules: the DELETE's inner window-function
     * SELECT re-scans its whole band on every run even when there is nothing
     * left to delete. Scanning the 50-minute tier-1 band every 5 minutes is
     * cheap; re-scanning the 23-hour tier-2 and 29-day tier-3 bands at that
     * rate was sustained I/O plus index churn for near-zero deletions. Rows
     * age into the slower tiers at most one schedule period late, which only
     * means temporarily finer resolution there - never data loss.
     */
    @Scheduled(cron = "${measurement.thinning.cron:0 */5 * * * ?}")
    public void thinTier1() {
        ZonedDateTime now = ZonedDateTime.now();
        int removed = thinBand(tier1IntervalSeconds,
                now.minusMinutes(tier2AfterMinutes), now.minusMinutes(tier1AfterMinutes));
        log.info("Thinning tier 1 done: removed {} ({}s band)", removed, tier1IntervalSeconds);
    }

    /** Tier 2 ([1d, 1h) -> 60s). Default: hourly. */
    @Scheduled(cron = "${measurement.thinning.tier2.cron:0 11 * * * ?}")
    public void thinTier2() {
        ZonedDateTime now = ZonedDateTime.now();
        int removed = thinBand(tier2IntervalSeconds,
                now.minusMinutes(tier3AfterMinutes), now.minusMinutes(tier2AfterMinutes));
        log.info("Thinning tier 2 done: removed {} ({}s band)", removed, tier2IntervalSeconds);
    }

    /** Tier 3 ([retention, 1d) -> 300s). Default: daily, after the 02:00 cleanup. */
    @Scheduled(cron = "${measurement.thinning.tier3.cron:0 23 3 * * ?}")
    public void thinTier3() {
        ZonedDateTime now = ZonedDateTime.now();
        int removed = thinBand(tier3IntervalSeconds,
                now.minusDays(retentionDays), now.minusMinutes(tier3AfterMinutes));
        log.info("Thinning tier 3 done: removed {} ({}s band)", removed, tier3IntervalSeconds);
    }

    /**
     * Thins a single band, isolating failures so one band cannot abort the others.
     *
     * @return the number of deleted measurements, or 0 if the band failed.
     */
    private int thinBand(long intervalSeconds, ZonedDateTime start, ZonedDateTime end) {
        try {
            return measurementRepository.thinBucket(intervalSeconds, start, end);
        } catch (Exception e) {
            log.error("Error thinning {}s band [{}, {})", intervalSeconds, start, end, e);
            return 0;
        }
    }
}
