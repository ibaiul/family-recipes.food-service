package eus.ibai.family.recipes.food.wm.infrastructure.config;

import org.axonframework.commandhandling.distributed.CommandBusConnector;
import org.axonframework.commandhandling.distributed.CommandRouter;
import org.axonframework.commandhandling.distributed.DistributedCommandBus;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@EnableDiscoveryClient
@Configuration
class ZookeeperConfig {

    @Bean("distributedCommandBus")
    @Primary
    public DistributedCommandBus distributedCommandBus(CommandRouter commandRouter,
                                                       CommandBusConnector commandBusConnector) {
        return DistributedCommandBus.builder()
                .commandRouter(commandRouter)
                .connector(commandBusConnector)
                .build();
    }
}