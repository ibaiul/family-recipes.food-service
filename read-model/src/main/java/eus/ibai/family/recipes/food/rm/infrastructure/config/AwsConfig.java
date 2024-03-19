package eus.ibai.family.recipes.food.rm.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

@Configuration
public class AwsConfig {

    @Bean
    public S3AsyncClient s3client(S3Properties s3Properties) throws URISyntaxException {
        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
                .writeTimeout(Duration.ZERO)
                .maxConcurrency(64)
                .build();
        S3Configuration serviceConfiguration = S3Configuration.builder()
                .checksumValidationEnabled(false)
                .chunkedEncodingEnabled(true)
                .pathStyleAccessEnabled(true)
                .build();
        return S3AsyncClient.builder().httpClient(httpClient)
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(s3Properties.getAccessKey(), s3Properties.getSecretKey())))
                .serviceConfiguration(serviceConfiguration)
                .endpointOverride(new URI(s3Properties.getEndpoint()))
                .build();
    }
}
