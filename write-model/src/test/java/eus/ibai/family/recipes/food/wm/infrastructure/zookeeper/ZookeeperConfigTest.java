package eus.ibai.family.recipes.food.wm.infrastructure.zookeeper;

import eus.ibai.family.recipes.food.wm.infrastructure.config.ZookeeperConfig;
import eus.ibai.family.recipes.food.wm.test.ZookeeperUtils;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.zookeeper.CuratorFrameworkCustomizer;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZookeeperConfigTest {

    private final ZookeeperConfig zookeeperConfig = new ZookeeperConfig();

    @Test
    void should_fail_if_cannot_determine_client_ssl_identity() throws Exception {
        File keystoreFile = Files.createTempFile("keystore-", ".jks").toFile();
        keystoreFile.deleteOnExit();
        KeyStore keystore = ZookeeperUtils.createEmptyKeystore();
        ZookeeperUtils.persistKeystore(keystore, keystoreFile);
        File truststoreFile = Files.createTempFile("truststore-", ".jks").toFile();
        truststoreFile.deleteOnExit();
        File clientProperties = Files.createTempFile("client", ".properties").toFile();
        clientProperties.deleteOnExit();
        ZookeeperUtils.createClientProperties(clientProperties, keystoreFile, truststoreFile);
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
        CuratorFrameworkCustomizer curatorFrameworkCustomizer = zookeeperConfig.customizeZookeeperClient(clientProperties.getAbsolutePath());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> curatorFrameworkCustomizer.customize(builder));
        assertThat(exception.getCause().getMessage()).isEqualTo("Could not determine x509 Principal ID.");
    }
}
