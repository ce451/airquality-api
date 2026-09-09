package com.elstner.airqualityapi.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Evenly strided downsampling for time-series responses. A chart that is
 * ~1200px wide cannot display more points than pixels, so transferring more
 * than {@code maxPoints} is wasted bandwidth on slow links.
 */
public final class MeasurementSampler {

    private MeasurementSampler() {
    }

    /**
     * Returns every k-th element so the result stays at (about) {@code maxPoints}
     * elements. The first element (newest, lists are ordered newest-first) is
     * always kept; the last element (oldest) is appended if the stride skipped
     * it, so the chart keeps its full time range. Order is preserved.
     *
     * @param points    the full series (any order, typically newest-first)
     * @param maxPoints target size; {@code null} or {@code <= 0} disables sampling
     * @return the original list if no sampling is needed, otherwise a new list
     *         with at most {@code maxPoints + 1} elements
     */
    public static <T> List<T> sample(List<T> points, Integer maxPoints) {
        if (maxPoints == null || maxPoints <= 0 || points.size() <= maxPoints) {
            return points;
        }

        int n = points.size();
        int stride = (int) Math.ceil((double) n / maxPoints);
        List<T> sampled = new ArrayList<>(n / stride + 2);
        for (int i = 0; i < n; i += stride) {
            sampled.add(points.get(i));
        }
        T oldest = points.get(n - 1);
        if (sampled.get(sampled.size() - 1) != oldest) {
            sampled.add(oldest);
        }
        return sampled;
    }
}
