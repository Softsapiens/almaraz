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

import io.micrometer.core.instrument.AbstractMeter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.util.MeterEquivalence;
import io.opentelemetry.api.trace.Span;
import io.prometheus.client.exemplars.CounterExemplarSampler;
import io.prometheus.client.exemplars.DefaultExemplarSampler;
import io.prometheus.client.exemplars.Exemplar;
import io.prometheus.client.exemplars.ExemplarConfig;
import io.prometheus.client.exemplars.tracer.common.SpanContextSupplier;
import io.prometheus.client.exemplars.tracer.otel.OpenTelemetrySpanContextSupplier;
import io.prometheus.client.exemplars.tracer.otel_agent.OpenTelemetryAgentSpanContextSupplier;

import java.util.concurrent.atomic.DoubleAdder;

public class PrometheusCounter extends AbstractMeter implements Counter {
    private DoubleAdder count = new DoubleAdder();
    private Exemplar exemplar = null;

    PrometheusCounter(Id id) {
        super(id);
    }

    @Override
    public void increment(double amount) {
        if (amount > 0) {
            if (ExemplarConfig.isExemplarsEnabled())
                this.exemplar = ExemplarConfig.getCounterExemplarSampler().sample(amount, this.exemplar);

            count.add(amount);
        }
    }

    @Override
    public double count() {
        return count.doubleValue();
    }

    public Exemplar exemplar() {
        return this.exemplar;
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return MeterEquivalence.equals(this, o);
    }

    @Override
    public int hashCode() {
        return MeterEquivalence.hashCode(this);
    }

    private Object findSpanContextSupplier() {
        try {
            if (OpenTelemetrySpanContextSupplier.isAvailable()) {
                return new OpenTelemetrySpanContextSupplier();
            }
        } catch (NoClassDefFoundError ignored) {
            // tracer_otel dependency not found
        } catch (UnsupportedClassVersionError ignored) {
            // OpenTelemetry requires Java 8, but client_java might run on Java 6.
        }
        try {
            if (OpenTelemetryAgentSpanContextSupplier.isAvailable()) {
                return new OpenTelemetryAgentSpanContextSupplier();
            }
        } catch (NoClassDefFoundError ignored) {
            // tracer_otel_agent dependency not found
        } catch (UnsupportedClassVersionError ignored) {
            // OpenTelemetry requires Java 8, but client_java might run on Java 6.
        }
        return null;
    }

}
