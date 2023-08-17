package eus.ibai.family.recipes.food.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.UUID;

@RequiredArgsConstructor
@Configuration
@AutoConfigureBefore({CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class})
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@ConditionalOnProperty(name = "newrelic.enabled", havingValue = "true")
public class NewRelicAutoConfiguration {

    private final NewRelicProperties newRelicProperties;

    private final MetricProperties metricProperties;

    @Bean
    public OpenTelemetry openTelemetry() throws UnknownHostException {
        String instanceId = UUID.randomUUID().toString();
        return OpenTelemetrySdk.builder()
                .setMeterProvider(
                        SdkMeterProvider.builder()
                                .setResource(
                                        Resource.getDefault().toBuilder()
                                                .put("service.name", newRelicProperties.getApp().name())
                                                .put("service.instance.id", instanceId)
                                                .put("instrumentation.provider", "micrometer")
                                                .put("host", InetAddress.getLocalHost().getHostName())
                                                .build())
                                .registerMetricReader(
                                        PeriodicMetricReader.builder(
                                                        OtlpGrpcMetricExporter.builder()
                                                                .setEndpoint(newRelicProperties.getMetrics().ingestUri())
                                                                .addHeader("api-key", newRelicProperties.getKeys().license())
                                                                .setAggregationTemporalitySelector(
                                                                        AggregationTemporalitySelector.deltaPreferred())
                                                                .build())
                                                .setInterval(Duration.ofSeconds(newRelicProperties.getMetrics().step()))
                                                .build())
                                .build())
                .build();
    }

    @Bean
    public MeterRegistry meterRegistry(OpenTelemetry openTelemetry) {
        return OpenTelemetryMeterRegistry.builder(openTelemetry).build();
    }

    @Bean
    @ConditionalOnProperty(name = "management.metrics.exclude[0]")
    MeterRegistryCustomizer<MeterRegistry> excludeMetrics() {
        return registry -> {
            for (String excludedMetricPrefix : metricProperties.getMetrics().exclude()) {
                registry.config().meterFilter(MeterFilter.denyNameStartsWith(excludedMetricPrefix));
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "management.observations.exclude[0]")
    ObservationRegistryCustomizer<ObservationRegistry> excludeObservations() {
        return registry -> {
            for (String excludedObservationPrefix : metricProperties.getObservations().exclude()) {
                registry.observationConfig().observationPredicate(denyNameStartsWith(excludedObservationPrefix));
            }
        };
    }
    
    private ObservationPredicate denyNameStartsWith(String prefix) {
        return (name, context) -> !name.startsWith(prefix);
    }
}
