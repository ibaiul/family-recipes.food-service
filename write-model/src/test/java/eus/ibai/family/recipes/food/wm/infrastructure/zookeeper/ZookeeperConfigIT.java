package eus.ibai.family.recipes.food.wm.infrastructure.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.zookeeper.discovery.ZookeeperDiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Temporarily disabled to determine why it fails on GitHub only")
@ActiveProfiles("axon-distributed")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ZookeeperConfigIT {

    @Container
    private static final ZookeeperContainer<?> zookeeperContainer = new ZookeeperContainer<>("bitnami/zookeeper")
            .withSsl()
            .withAuth()
            .withReuse(true);

    @AfterAll
    static void afterAll() {
        zookeeperContainer.stop();
    }

    @Autowired
    private CuratorFramework curatorFramework;

    @Autowired
    private DiscoveryClient discoveryClient;

    @LocalServerPort
    private int randomServerPort;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void should_connect_to_ssl_port() {
        assertThat(curatorFramework.getZookeeperClient().isConnected()).isTrue();
    }

    @Test
    void should_create_root_nodes_with_open_acls() throws Exception {
        ZooKeeper zooKeeper = curatorFramework.getZookeeperClient().getZooKeeper();
        List<ACL> acls = zooKeeper.getACL("/services", null);
        assertThat(acls).containsExactlyInAnyOrder(new ACL(ZooDefs.Perms.ALL, new Id("world", "anyone")));
    }

    @Test
    void should_create_child_nodes_with_restricted_acls() throws Exception {
        ZooKeeper zooKeeper = curatorFramework.getZookeeperClient().getZooKeeper();
        List<ACL> acls = zooKeeper.getACL("/services/serviceName", null);
        assertThat(acls).containsExactlyInAnyOrder(new ACL(ZooDefs.Perms.ALL, new Id("x509", "CN=client,OU=OU,O=O,L=Bilbo,ST=Bizkaia,C=XX")));
    }

    @Test
    void should_register_service_instance() {
        List<String> registeredServices = discoveryClient.getServices();
        assertThat(registeredServices).containsExactlyInAnyOrder("serviceName");
        List<ServiceInstance> registeredInstances = discoveryClient.getInstances(registeredServices.get(0));
        assertThat(registeredInstances).hasSize(1);
        assertThat(registeredInstances.get(0).getPort()).isEqualTo(randomServerPort);
    }

    @Test
    void should_only_load_zookeeper_discovery_client() {
        List<DiscoveryClient> discoveryClients = applicationContext.getBeansOfType(DiscoveryClient.class).values().stream().toList();
        assertThat(discoveryClients).hasSize(1);
        assertThat(discoveryClients.get(0)).isExactlyInstanceOf(ZookeeperDiscoveryClient.class);
    }

    @DynamicPropertySource
    public static void setDynamicProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.zookeeper.connect-string", () -> "localhost:" + zookeeperContainer.getHttpsPort());
        registry.add("spring.cloud.zookeeper.client-config", () -> zookeeperContainer.getClientProperties().getAbsolutePath());
    }
}
