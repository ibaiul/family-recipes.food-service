package eus.ibai.family.recipes.food.wm.infrastructure.config;

import org.apache.curator.framework.api.ACLProvider;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.admin.ZooKeeperAdmin;
import org.apache.zookeeper.client.ZKClientConfig;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.zookeeper.CuratorFrameworkCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EnableDiscoveryClient
@Configuration
@ConditionalOnProperty(name = "axon.distributed.enabled", havingValue = "true")
public class ZookeeperConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.cloud.zookeeper.client-config")
    public CuratorFrameworkCustomizer customizeZookeeperClient(@Value("${spring.cloud.zookeeper.client-config}") String clientConfig) {
        return builder -> {
            final ZKClientConfig zkClientConfig;
            final String principalId;
            try {
                zkClientConfig = new ZKClientConfig(clientConfig);
                String keystoreFile = zkClientConfig.getProperty("zookeeper.ssl.keyStore.location");
                String keystorePass = zkClientConfig.getProperty("zookeeper.ssl.keyStore.password");
                principalId = getPrincipalId(keystoreFile, keystorePass);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            builder
                    .zookeeperFactory((connectString, sessionTimeout, watcher, canBeReadOnly) -> new ZooKeeperAdmin(connectString, sessionTimeout, watcher, zkClientConfig))
                    .authorization("x509", principalId.getBytes())
                    .aclProvider(new ACLProvider() {
                        @Override
                        public List<ACL> getDefaultAcl() {
                            throw new IllegalStateException("ACL not defined for path.");
                        }

                        @Override
                        public List<ACL> getAclForPath(String path) {
                            if ("/services".equals(path)) {
                                return ZooDefs.Ids.OPEN_ACL_UNSAFE;
                            }
                            return Collections.singletonList(new ACL(ZooDefs.Perms.ALL, new Id("x509", principalId)));
                        }
                    });
        };
    }

    private String getPrincipalId(String keystoreFile, String keystorePass) throws GeneralSecurityException, IOException {
        KeyStore one = KeyStore.getInstance("JKS");
        try (InputStream is = new FileInputStream(keystoreFile)) {
            one.load(is, keystorePass.toCharArray());
            List<String> aliases = new ArrayList<>();
            one.aliases().asIterator().forEachRemaining(aliases::add);
            for (String alias : aliases) {
                Certificate cert = one.getCertificate(alias);
                if (cert instanceof X509Certificate x509Certificate && (!isCACert(x509Certificate))) {
                    return x509Certificate.getSubjectX500Principal().getName();
                }
            }
        }
        throw new IllegalStateException("Could not determine x509 Principal ID.");
    }

    private boolean isCACert(X509Certificate certificate) {
        return certificate.getBasicConstraints() != -1;
    }
}