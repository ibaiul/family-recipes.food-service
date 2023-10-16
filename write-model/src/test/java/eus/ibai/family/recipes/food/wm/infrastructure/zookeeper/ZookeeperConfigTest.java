package eus.ibai.family.recipes.food.wm.infrastructure.zookeeper;

import eus.ibai.family.recipes.food.wm.test.TlsUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class ZookeeperConfigTest {

    private static final ZookeeperContainer<?> zookeeperContainer;

    private static final File clientKeystore;
    private static final File clientTruststore;
    private static final File clientProperties;


    static {
        File serverKeystore;
        File serverTruststore;
//        Set<PosixFilePermission> perms = new HashSet<>();
//        perms.add(PosixFilePermission.);
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-r--r--");
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
        try {
            serverKeystore = Files.createTempFile("server-keystore-", ".jks", attr).toFile();
//            serverKeystore.deleteOnExit();
            serverTruststore = Files.createTempFile("server-truststore-", ".jks", attr).toFile();
//            serverTruststore.deleteOnExit();
            clientKeystore = Files.createTempFile("client-keystore-", ".jks").toFile();
//            clientKeystore.deleteOnExit();
            clientTruststore = Files.createTempFile("client-truststore-", ".jks").toFile();
//            clientTruststore.deleteOnExit();
            TlsUtils.createKeystore(serverKeystore, serverTruststore, clientKeystore, clientTruststore);

            clientProperties = Files.createTempFile("client", ".properties").toFile();
            TlsUtils.createClientProperties(clientProperties, clientKeystore, clientTruststore);
//            clientProperties.deleteOnExit();

//            serverKeystore = new File("/home/ibai/git/utils/docker-compose/zookeeper/keystore.jks");
//            serverTruststore = new File("/home/ibai/git/utils/docker-compose/zookeeper/truststore.jks");
//            clientKeystore = new File("/home/ibai/git/utils/docker-compose/zookeeper/client-keystore.jks");
//            clientTruststore = new File("/home/ibai/git/utils/docker-compose/zookeeper/client-truststore.jks");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        zookeeperContainer = new ZookeeperContainer<>("bitnami/zookeeper")
                .withSsl(serverKeystore, "123456", serverTruststore, "123456")
                .withAuth()
                .withReuse(true);
        zookeeperContainer.start();
    }

//    private final ZookeeperConfig zookeeperConfig = new ZookeeperConfig();

    @Autowired
    private CuratorFramework curatorFramework;

    @Test
    void happy_path() throws Exception {
//        File propertyFile = Files.createTempFile("client", ".properties").toFile();
//        TlsUtils.createClientProperties(propertyFile, clientKeystore, clientTruststore);
//        CuratorFrameworkCustomizer curatorFrameworkCustomizer = zookeeperConfig.customizeZookeeperClient(propertyFile.getAbsolutePath());
//        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
//                .retryPolicy(new RetryOneTime(1))
//                .connectString(zookeeperContainer.getHost() + ":3181");
//
//        curatorFrameworkCustomizer.customize(builder);
//        CuratorFramework curatorFramework = builder.build();
//        curatorFramework.start();
////        curatorFramework.getZookeeperClient().start();

        // TODO Run as SpringBootTest and then we should be able to assert existing ACLs -> curatorFramework.getACL().forPath("/services");
        assertThat(curatorFramework.getZookeeperClient().blockUntilConnectedOrTimedOut()).isTrue();
        assertThat(curatorFramework.getZookeeperClient().isConnected()).isTrue();
        curatorFramework.getACL().forPath("/services");
        ZooKeeper zooKeeper = curatorFramework.getZookeeperClient().getZooKeeper();
        List<ACL> acls = zooKeeper.getACL("/services", null);
        assertThat(acls).containsExactlyInAnyOrder(new ACL(ZooDefs.Perms.ALL, new Id("world", "anyone")));
        acls = zooKeeper.getACL("/services/serviceName", null);
        assertThat(acls).containsExactlyInAnyOrder(new ACL(ZooDefs.Perms.ALL, new Id("x509", "CN=client,OU=OU,O=O,L=Bilbo,ST=Bizkaia,C=XX")));
        zooKeeper.getClientConfig();
        System.out.println("SERVUS!!");
    }

    @DynamicPropertySource
    public static void setDatasourceProperties(final DynamicPropertyRegistry registry) {
//        registry.add("spring.cloud.zookeeper.connect-string", () -> "localhost:" + zookeeperContainer.getHttpPort());
        registry.add("spring.cloud.zookeeper.connect-string", () -> "localhost:" + zookeeperContainer.getHttpsPort());
//        registry.add("spring.cloud.zookeeper.connect-string", () -> "localhost:3181");
        registry.add("spring.cloud.zookeeper.client-config", clientProperties::getAbsolutePath);
    }
}
