package eus.ibai.family.recipes.food.wm.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;

@EnableDiscoveryClient
@Configuration
@ConditionalOnProperty(name = "axon.distributed.enabled", havingValue = "true")
class ZookeeperConfig {}