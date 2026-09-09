package com.elstner.airqualityapi.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class MeasurementSamplerTest {

    private static List<Integer> series(int n) {
        return IntStream.range(0, n).boxed().toList();
    }

    @Test
    void nullOrNonPositiveMaxPointsDisablesSampling() {
        List<Integer> points = series(100);
        assertThat(MeasurementSampler.sample(points, null)).isSameAs(points);
        assertThat(MeasurementSampler.sample(points, 0)).isSameAs(points);
        assertThat(MeasurementSampler.sample(points, -5)).isSameAs(points);
    }

    @Test
    void seriesSmallerThanOrEqualToMaxPointsIsReturnedUnchanged() {
        List<Integer> points = series(100);
        assertThat(MeasurementSampler.sample(points, 100)).isSameAs(points);
        assertThat(MeasurementSampler.sample(points, 500)).isSameAs(points);
    }

    @Test
    void samplesDownToAtMostMaxPointsPlusOldest() {
        List<Integer> sampled = MeasurementSampler.sample(series(1521), 500);
        assertThat(sampled.size()).isBetween(2, 501);
        // newest (first) and oldest (last) survive, order is preserved
        assertThat(sampled.get(0)).isZero();
        assertThat(sampled.get(sampled.size() - 1)).isEqualTo(1520);
        assertThat(sampled).isSorted();
    }

    @Test
    void strideIsEven() {
        // 1000 points, max 100 -> stride 10: 0, 10, 20, ... 990, plus oldest 999
        List<Integer> sampled = MeasurementSampler.sample(series(1000), 100);
        assertThat(sampled).startsWith(0, 10, 20);
        assertThat(sampled).endsWith(990, 999);
        assertThat(sampled).hasSize(101);
    }

    @Test
    void oldestIsNotDuplicatedWhenStrideLandsOnIt() {
        // 10 points, max 5 -> stride 2: 0,2,4,6,8 plus oldest 9
        List<Integer> sampled = MeasurementSampler.sample(series(10), 5);
        assertThat(sampled).containsExactly(0, 2, 4, 6, 8, 9);
        // 9 points, max 3 -> stride 3: 0,3,6 plus oldest 8
        assertThat(MeasurementSampler.sample(series(9), 3)).containsExactly(0, 3, 6, 8);
        // stride lands exactly on the last element -> no duplicate
        // 7 points, max 4 -> stride 2: 0,2,4,6; 6 is already the oldest
        assertThat(MeasurementSampler.sample(series(7), 4)).containsExactly(0, 2, 4, 6);
    }

    @Test
    void tinySeriesEdgeCases() {
        assertThat(MeasurementSampler.sample(series(0), 5)).isEmpty();
        assertThat(MeasurementSampler.sample(series(1), 5)).containsExactly(0);
        assertThat(MeasurementSampler.sample(series(2), 1)).containsExactly(0, 1);
    }
}
