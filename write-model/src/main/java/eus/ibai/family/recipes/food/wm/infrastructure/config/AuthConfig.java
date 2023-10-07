package eus.ibai.family.recipes.food.wm.infrastructure.config;

import eus.ibai.family.recipes.food.auth.service.LocalServiceTokenAuthenticator;
import eus.ibai.family.recipes.food.auth.service.PeriodicallyRefreshedServiceTokenProvider;
import eus.ibai.family.recipes.food.auth.service.ServiceTokenAuthenticator;
import eus.ibai.family.recipes.food.auth.service.ServiceTokenProvider;
import eus.ibai.family.recipes.food.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
@ConditionalOnProperty(name = "axon.distributed.enabled", havingValue = "true")
public class AuthConfig {

    @Bean
    ServiceTokenProvider serviceTokenProvider(ServiceTokenAuthenticator serviceTokenAuthenticator, @Value("${services.self.refresh-interval}") long refreshInterval) {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        PeriodicallyRefreshedServiceTokenProvider serviceTokenProvider = new PeriodicallyRefreshedServiceTokenProvider(serviceTokenAuthenticator, scheduledThreadPoolExecutor, refreshInterval);
        serviceTokenProvider.init();
        return serviceTokenProvider;
    }

    @Bean
    ServiceTokenAuthenticator serviceTokenAuthenticator(@Value("${services.self.id}") String serviceId, JwtService jwtService) {
        return new LocalServiceTokenAuthenticator(serviceId, jwtService);
    }
}
