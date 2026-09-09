package com.elstner.airqualityapi.service;

import com.elstner.airqualityapi.repository.MeasurementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for the band/boundary logic of {@link MeasurementThinningService}.
 * The actual SQL behaviour (idempotency, "keep earliest per bucket") is covered by
 * {@link com.elstner.airqualityapi.repository.MeasurementRepositoryThinningTest}.
 */
@ExtendWith(MockitoExtension.class)
class MeasurementThinningServiceTest {

    @Mock
    private MeasurementRepository measurementRepository;

    @InjectMocks
    private MeasurementThinningService service;

    @BeforeEach
    void configureTiers() {
        ReflectionTestUtils.setField(service, "retentionDays", 30);
        ReflectionTestUtils.setField(service, "tier1AfterMinutes", 10L);
        ReflectionTestUtils.setField(service, "tier1IntervalSeconds", 30L);
        ReflectionTestUtils.setField(service, "tier2AfterMinutes", 60L);
        ReflectionTestUtils.setField(service, "tier2IntervalSeconds", 60L);
        ReflectionTestUtils.setField(service, "tier3AfterMinutes", 1440L);
        ReflectionTestUtils.setField(service, "tier3IntervalSeconds", 300L);
    }

    /**
     * Runs one tier and asserts its band edges. Boundaries are bracketed with
     * before/after using the same arithmetic the service uses (minus
     * minutes/days), so DST can't make it flaky.
     */
    private void assertBand(Runnable tier, long expectedInterval,
                            java.util.function.Function<ZonedDateTime, ZonedDateTime> startEdge,
                            java.util.function.Function<ZonedDateTime, ZonedDateTime> endEdge) {
        when(measurementRepository.thinBucket(anyLong(), any(), any())).thenReturn(0);

        ZonedDateTime before = ZonedDateTime.now();
        tier.run();
        ZonedDateTime after = ZonedDateTime.now();

        ArgumentCaptor<ZonedDateTime> start = ArgumentCaptor.forClass(ZonedDateTime.class);
        ArgumentCaptor<ZonedDateTime> end = ArgumentCaptor.forClass(ZonedDateTime.class);
        verify(measurementRepository).thinBucket(eq(expectedInterval), start.capture(), end.capture());

        assertThat(start.getValue()).isBetween(startEdge.apply(before), startEdge.apply(after));
        assertThat(end.getValue()).isBetween(endEdge.apply(before), endEdge.apply(after));
        assertThat(start.getValue()).isBefore(end.getValue()); // older edge -> younger edge
    }

    @Test
    void tier1ThinsTheTenMinuteToOneHourBandAt30s() {
        assertBand(service::thinTier1, 30L,
                now -> now.minusMinutes(60), now -> now.minusMinutes(10));
    }

    @Test
    void tier2ThinsTheOneHourToOneDayBandAt60s() {
        assertBand(service::thinTier2, 60L,
                now -> now.minusMinutes(1440), now -> now.minusMinutes(60));
    }

    @Test
    void tier3ThinsTheOneDayToRetentionBandAt300s() {
        assertBand(service::thinTier3, 300L,
                now -> now.minusDays(30), now -> now.minusMinutes(1440));
    }

    @Test
    void swallowsRepositoryExceptionsSoTheSchedulerKeepsRunning() {
        when(measurementRepository.thinBucket(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("db unavailable"));

        assertThatCode(() -> {
            service.thinTier1();
            service.thinTier2();
            service.thinTier3();
        }).doesNotThrowAnyException();
    }
}
