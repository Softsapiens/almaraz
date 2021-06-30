/**
 * Copyright 2017 VMware, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.prometheus;

import io.micrometer.core.instrument.AbstractTimer;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.distribution.*;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.core.instrument.util.TimeUtils;
import io.micrometer.core.lang.Nullable;
import io.prometheus.client.exemplars.DefaultExemplarSampler;
import io.prometheus.client.exemplars.Exemplar;
import io.prometheus.client.exemplars.ExemplarConfig;
import io.prometheus.client.exemplars.tracer.common.SpanContextSupplier;
import io.prometheus.client.exemplars.tracer.otel.OpenTelemetrySpanContextSupplier;
import io.prometheus.client.exemplars.tracer.otel_agent.OpenTelemetryAgentSpanContextSupplier;

import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class PrometheusTimer extends AbstractTimer {
    private static final CountAtBucket[] EMPTY_HISTOGRAM = new CountAtBucket[0];

    private final LongAdder count = new LongAdder();
    private final LongAdder totalTime = new LongAdder();
    private final TimeWindowMax max;

    private final HistogramFlavor histogramFlavor;

    private final HashMap<Double, Exemplar> exemplars = new HashMap<>();

    @Nullable
    private final Histogram histogram;

    PrometheusTimer(Id id, Clock clock, DistributionStatisticConfig distributionStatisticConfig, PauseDetector pauseDetector, HistogramFlavor histogramFlavor) {
        super(id, clock,
                DistributionStatisticConfig.builder()
                        .percentilesHistogram(false)
                        .serviceLevelObjectives()
                        .build()
                        .merge(distributionStatisticConfig),
                pauseDetector, TimeUnit.SECONDS, false);

        this.histogramFlavor = histogramFlavor;
        this.max = new TimeWindowMax(clock, distributionStatisticConfig);

        if (distributionStatisticConfig.isPublishingHistogram()) {
            switch (histogramFlavor) {
                case Prometheus:
                    histogram = new TimeWindowFixedBoundaryHistogram(clock, DistributionStatisticConfig.builder()
                            .expiry(Duration.ofDays(1825)) // effectively never roll over
                            .bufferLength(1)
                            .build()
                            .merge(distributionStatisticConfig), true);
                    break;
                case VictoriaMetrics:
                    histogram = new FixedBoundaryVictoriaMetricsHistogram();
                    break;
                default:
                    histogram = null;
                    break;
            }
        } else {
            histogram = null;
        }
    }

    @Override
    protected void recordNonNegative(long amount, TimeUnit unit) {
        count.increment();
        long nanoAmount = TimeUnit.NANOSECONDS.convert(amount, unit);
        totalTime.add(nanoAmount);
        max.record(nanoAmount, TimeUnit.NANOSECONDS);

        if (histogram != null) {
            histogram.recordLong(TimeUnit.NANOSECONDS.convert(amount, unit));

            Exemplar exemplar = (ExemplarConfig.isExemplarsEnabled() ?
                    ExemplarConfig.getHistogramExemplarSampler().sample(Long.valueOf(nanoAmount).doubleValue(), 0, 0, null)
                    :null);

            if (exemplar!=null) {
            /*
            A solution could be to store exemplars by its nanoseconds amount.
            Then inside PrometheusMeterRegistry.addTimerSamples(...) associate each bucket (value) with its nanoAmount-exemplar.
            Remember that this condition (countAtBucket.bucket(TimeUnit.NANOSECONDS)>=nanoAmount) stands to find the first bucket where an exemplar belongs.
             */
                CountAtBucket[] counts = histogramCounts();
                System.out.println("Total number of buckets " + counts.length);
                for (CountAtBucket countAtBucket : counts) {
                    if (countAtBucket.bucket(TimeUnit.NANOSECONDS) >= nanoAmount) {
                        this.exemplars.put(countAtBucket.bucket(TimeUnit.NANOSECONDS), exemplar);
                        System.out.println(countAtBucket + " having exemplar " + exemplar.getValue());
                    }
                }
            }
        }

        System.out.println("[PrometheusTimer][" + this.getId() + "] recording non negative value with exemplars");
    }

    public Exemplar getExemplar(Double value) {
        return this.exemplars.get(value);
    }

    @Override
    public long count() {
        return count.longValue();
    }

    @Override
    public double totalTime(TimeUnit unit) {
        return TimeUtils.nanosToUnit(totalTime.doubleValue(), unit);
    }

    @Override
    public double max(TimeUnit unit) {
        return max.poll(unit);
    }

    public HistogramFlavor histogramFlavor() {
        return histogramFlavor;
    }

    /**
     * For Prometheus we cannot use the histogram counts from HistogramSnapshot, as it is based on a
     * rolling histogram. Prometheus requires a histogram that accumulates values over the lifetime of the app.
     *
     * @return Cumulative histogram buckets.
     */
    public CountAtBucket[] histogramCounts() {
        return histogram == null ? EMPTY_HISTOGRAM : histogram.takeSnapshot(0, 0, 0).histogramCounts();
    }

    @Override
    public HistogramSnapshot takeSnapshot() {
        HistogramSnapshot snapshot = super.takeSnapshot();

        if (histogram == null) {
            return snapshot;
        }

        return new HistogramSnapshot(snapshot.count(),
                snapshot.total(),
                snapshot.max(),
                snapshot.percentileValues(),
                histogramCounts(),
                snapshot::outputSummary);
    }
}
