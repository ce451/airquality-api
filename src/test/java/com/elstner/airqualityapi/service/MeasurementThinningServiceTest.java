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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for the cascade/boundary logic of {@link MeasurementThinningService}.
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

    @Test
    void thinsThreeContiguousBandsWithCascadingResolution() {
        when(measurementRepository.thinBucket(anyLong(), any(), any())).thenReturn(0);

        ZonedDateTime before = ZonedDateTime.now();
        service.thinMeasurements();
        ZonedDateTime after = ZonedDateTime.now();

        ArgumentCaptor<Long> interval = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<ZonedDateTime> start = ArgumentCaptor.forClass(ZonedDateTime.class);
        ArgumentCaptor<ZonedDateTime> end = ArgumentCaptor.forClass(ZonedDateTime.class);
        verify(measurementRepository, times(3)).thinBucket(interval.capture(), start.capture(), end.capture());

        List<Long> intervals = interval.getAllValues();
        List<ZonedDateTime> starts = start.getAllValues();
        List<ZonedDateTime> ends = end.getAllValues();

        // Resolution cascade, finest band first: 30s -> 60s -> 300s.
        assertThat(intervals).containsExactly(30L, 60L, 300L);

        // Each band boundary is computed off the same "now"; bracket it with before/after.
        // Same arithmetic the service uses (minus minutes/days), so DST can't make it flaky.
        assertThat(ends.get(0)).isBetween(before.minusMinutes(10), after.minusMinutes(10));     // band1 end   = now-10min
        assertThat(starts.get(0)).isBetween(before.minusMinutes(60), after.minusMinutes(60));   // band1 start = now-1h
        assertThat(starts.get(1)).isBetween(before.minusMinutes(1440), after.minusMinutes(1440)); // band2 start = now-1day
        assertThat(starts.get(2)).isBetween(before.minusDays(30), after.minusDays(30));         // band3 start = now-retention

        // Bands are contiguous and non-overlapping: the younger band starts where the older one ends.
        assertThat(starts.get(0)).isEqualTo(ends.get(1));
        assertThat(starts.get(1)).isEqualTo(ends.get(2));

        // Every band runs older edge -> younger edge.
        assertThat(starts.get(0)).isBefore(ends.get(0));
        assertThat(starts.get(1)).isBefore(ends.get(1));
        assertThat(starts.get(2)).isBefore(ends.get(2));
    }

    @Test
    void swallowsRepositoryExceptionsSoTheSchedulerKeepsRunning() {
        when(measurementRepository.thinBucket(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("db unavailable"));

        assertThatCode(() -> service.thinMeasurements()).doesNotThrowAnyException();
    }
}
