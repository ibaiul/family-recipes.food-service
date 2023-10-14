package eus.ibai.family.recipes.food.wm.infrastructure.config;

import org.apache.commons.lang.NotImplementedException;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.utils.ZookeeperFactory;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.admin.ZooKeeperAdmin;
import org.apache.zookeeper.client.ZKClientConfig;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@EnableDiscoveryClient
@Configuration
//@BootstrapConfiguration
@ConditionalOnProperty(name = "axon.distributed.enabled", havingValue = "true")
class ZookeeperConfig {


    @Bean
    public CuratorFrameworkCustomizer customizeZookeeperClient(@Value("${spring.cloud.zookeeper.user}") String user,
                                                               @Value("${spring.cloud.zookeeper.pass}") String pass,
                                                               @Value("${spring.cloud.zookeeper.client-config}") String clientConfig) {
        return builder -> {
            final ZKClientConfig zkClientConfig;
            final String principalId;
            try {
                zkClientConfig = new ZKClientConfig(clientConfig);
                String keystoreFile = zkClientConfig.getProperty("zookeeper.ssl.keyStore.location");
                String keystorePass = zkClientConfig.getProperty("zookeeper.ssl.keyStore.password");
                principalId = getPrincipalId(keystoreFile, keystorePass);
            } catch (QuorumPeerConfig.ConfigException | GeneralSecurityException | IOException e) {
                throw new IllegalStateException(e);
            }
            builder
                    .zookeeperFactory(new ZookeeperFactory() {
                        @Override
                        public ZooKeeper newZooKeeper(String connectString, int sessionTimeout, Watcher watcher, boolean canBeReadOnly) throws Exception {
//                            return new ZooKeeperAdmin(connectString, sessionTimeout, watcher, canBeReadOnly);
                            // TODO This config is the key to SASL + SSL
                            // ALTERNATIVE: zookeeper.sasl.client=Client + jaas config file with Client root element with credentials
                            // Try for digest SASL -> zkClientConfig.getJaasConfKey() should get the SASL conf -> test with docker-compose an dprint the jaas system property just in case
//                            ZKClientConfig zkClientConfig = new ZKClientConfig("/home/ibai/git/utils/docker-compose/zookeeper/client.properties");
                            return new ZooKeeperAdmin(connectString, sessionTimeout, watcher, zkClientConfig);
                        }
                    })
//                    .authorization("digest", (user + ":" + pass).getBytes()) // TODO Test passing --Djava.security.auth.login.config=/home/ibai/git/utils/docker-compose/zookeeper/client-jaas.conf
//                    .authorization("x509", "CN=family-recipes.food-service.command,OU=OU,O=O,L=Bilbo,ST=Bizkaia,C=EH".getBytes())
                    .authorization("x509", principalId.getBytes())
//                    .authorization("digest", generateMd5Digest(user + ":" + pass))
                    .aclProvider(new ACLProvider() {
                        @Override
                        public List<ACL> getDefaultAcl() {
//                            return Arrays.asList(new ACL(ZooDefs.Perms.WRITE, new Id("digest", "sasl:zoo:crdwa")));
//                            return Arrays.asList(new ACL(ZooDefs.Perms.ADMIN, new Id("digest", generateDigest(user + ":" + pass))));
                            throw new NotImplementedException("");
                        }

                        @Override
                        public List<ACL> getAclForPath(String path) {
                            if ("/services".equals(path)) {
                                return ZooDefs.Ids.OPEN_ACL_UNSAFE;
                            }
//                            return Collections.singletonList(new ACL(ZooDefs.Perms.ALL, new Id("digest", "sasl:zoo:crdwa"))); // InvalidAcl
//                            return Collections.singletonList(new ACL(ZooDefs.Perms.ALL, new Id("digest", "zoo"))); // InvalidAcl
//                            return Collections.singletonList(new ACL(ZooDefs.Perms.ALL, new Id("digest", "sasl:zoo"))); // NoAuth
//                            return Collections.singletonList(new ACL(ZooDefs.Perms.ALL, new Id("digest", "zoo:crdwa"))); // NoAuth
//                            return Arrays.asList(new ACL(ZooDefs.Perms.WRITE, new Id("digest", "sasl:zoo:crdwa")));
//                            return Arrays.asList(new ACL(ZooDefs.Perms.ADMIN, new Id("digest", generateDigest(user + ":" + pass))));
//                            return Arrays.asList(new ACL(String name =ZooDefs.Perms.ADMIN, new Id("digest", user + ":" + generateDigest2(pass))));

//                            return Collections.singletonList(new ACL(ZooDefs.Perms.ALL, new Id("x509", "CN=family-recipes.food-service.command,OU=OU,O=O,L=Bilbo,ST=Bizkaia,C=EH")));
                            return Collections.singletonList(new ACL(ZooDefs.Perms.ALL, new Id("x509", principalId)));
                        }
                    })
            ;
        };
    }

    private String getPrincipalId(String keystoreFile, String keystorePass) throws GeneralSecurityException, IOException {
        KeyStore one = KeyStore.getInstance("JKS"); // optionally add ,provider
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

    public static String generateDigest2(String password) {
        byte[] digest = new byte[0];
        try {
            digest = MessageDigest.getInstance("SHA1").digest(password.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return base64Encode(digest);
    }

    public static String generateDigest(String idPassword) {
        String[] parts = idPassword.split(":", 2);
        byte[] digest = new byte[0];
        try {
            digest = MessageDigest.getInstance("SHA1").digest(idPassword.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return parts[0] + ":" + base64Encode(digest);
    }

    public static byte[] generateMd5Digest(String idPassword) {
        String[] parts = idPassword.split(":", 2);
        byte[] digest = new byte[0];
        try {
            digest = MessageDigest.getInstance("MD5").digest(idPassword.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return digest;
    }

    private static String base64Encode(byte[] byteDigest) {
        return new String(Base64.getEncoder().encode(byteDigest));
    }

//    @Bean
//    public CuratorFrameworkCustomizer customizeZookeeperClient() {
//        return new CuratorFrameworkCustomizer() {
//            @Override
//            public void customize(CuratorFrameworkFactory.Builder builder) {
//                builder.authorization("digest", "zoo:keeper".getBytes());
//            }
//        };
//    }
}