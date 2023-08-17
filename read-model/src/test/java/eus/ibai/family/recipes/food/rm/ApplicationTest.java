package eus.ibai.family.recipes.food.rm;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static eus.ibai.family.recipes.food.test.TestUtils.stubNewRelicSendMetricResponse;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ApplicationTest {

    @RegisterExtension
    private static final WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .configureStaticDsl(true)
            .build();

    @BeforeEach
    void beforeEach() {
        stubNewRelicSendMetricResponse();
    }

    @Test
    void application_starts() {
        assertDoesNotThrow(() -> Application.main(new String[]{"--newrelic.enabled=true", format("--newrelic.metrics.ingest-uri=%s/metric/v1", wiremock.baseUrl())}));
    }
}
