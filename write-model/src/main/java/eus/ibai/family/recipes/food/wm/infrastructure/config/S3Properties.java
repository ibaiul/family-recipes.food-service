package eus.ibai.family.recipes.food.wm.infrastructure.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties("s3")
public class S3Properties {

    private String endpoint;

    private String region;

    private String accessKey;

    private String secretKey;
}
