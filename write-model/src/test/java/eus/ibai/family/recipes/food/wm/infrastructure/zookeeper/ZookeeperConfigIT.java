package eus.ibai.family.recipes.food.wm.infrastructure.zookeeper;

import eus.ibai.family.recipes.food.wm.test.ZookeeperUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("axon-distributed")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ZookeeperConfigIT {

    private static final ZookeeperContainer<?> zookeeperContainer;

    private static final File clientKeystore;

    private static final File clientTruststore;

    private static final File clientProperties;

    static {
        File serverKeystore;
        File serverTruststore;
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-r--r--");
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
        try {
            serverKeystore = Files.createTempFile("server-keystore-", ".jks", attr).toFile();
            serverKeystore.deleteOnExit();
            serverTruststore = Files.createTempFile("server-truststore-", ".jks", attr).toFile();
            serverTruststore.deleteOnExit();
            clientKeystore = Files.createTempFile("client-keystore-", ".jks").toFile();
            clientKeystore.deleteOnExit();
            clientTruststore = Files.createTempFile("client-truststore-", ".jks").toFile();
            clientTruststore.deleteOnExit();
            ZookeeperUtils.createJksFiles(serverKeystore, serverTruststore, clientKeystore, clientTruststore);

            clientProperties = Files.createTempFile("client", ".properties").toFile();
            ZookeeperUtils.createClientProperties(clientProperties, clientKeystore, clientTruststore);
            clientProperties.deleteOnExit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        zookeeperContainer = new ZookeeperContainer<>("bitnami/zookeeper")
                .withSsl(serverKeystore, "123456", serverTruststore, "123456")
                .withAuth()
                .withReuse(true);
        zookeeperContainer.start();
    }

    @Autowired
    private CuratorFramework curatorFramework;

    @Autowired
    private DiscoveryClient discoveryClient;

    @LocalServerPort
    private int randomServerPort;

    @Test
    void should_connect_to_ssl_port() throws Exception {
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

    @DynamicPropertySource
    public static void setDynamicProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.zookeeper.connect-string", () -> "localhost:" + zookeeperContainer.getHttpsPort());
        registry.add("spring.cloud.zookeeper.client-config", clientProperties::getAbsolutePath);
    }
}
