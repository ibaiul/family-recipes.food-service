package eus.ibai.family.recipes.food.wm.infrastructure.config;

import org.apache.commons.lang.NotImplementedException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.utils.ZookeeperFactory;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.admin.ZooKeeperAdmin;
import org.apache.zookeeper.client.ZKClientConfig;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.bootstrap.BootstrapConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.zookeeper.CuratorFrameworkCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@EnableDiscoveryClient
@Configuration
//@BootstrapConfiguration
@ConditionalOnProperty(name = "axon.distributed.enabled", havingValue = "true")
class ZookeeperConfig {

    @Bean
    public CuratorFrameworkCustomizer customizeZookeeperClient(@Value("${spring.cloud.zookeeper.user}") String user, @Value("${spring.cloud.zookeeper.pass}") String pass) {
        return builder -> {
            builder
                    .zookeeperFactory(new ZookeeperFactory() {
                        @Override
                        public ZooKeeper newZooKeeper(String connectString, int sessionTimeout, Watcher watcher, boolean canBeReadOnly) throws Exception {
//                            return new ZooKeeperAdmin(connectString, sessionTimeout, watcher, canBeReadOnly);
                            // TODO This config is the key to SASL + SSL
                            ZKClientConfig zkClientConfig = new ZKClientConfig("/home/ibai/git/utils/docker-compose/zookeeper/client.properties");
                            return new ZooKeeperAdmin(connectString, sessionTimeout, watcher, zkClientConfig);
                        }
                    })
                    .authorization("digest", (user + ":" + pass).getBytes())
//                    .authorization("digest", generateMd5Digest(user + ":" + pass))
//                    .aclProvider(new ACLProvider() {
//                        @Override
//                        public List<ACL> getDefaultAcl() {
////                            return Arrays.asList(new ACL(ZooDefs.Perms.WRITE, new Id("digest", "sasl:zoo:crdwa")));
////                            return Arrays.asList(new ACL(ZooDefs.Perms.ADMIN, new Id("digest", generateDigest(user + ":" + pass))));
//                            throw new NotImplementedException("");
//                        }
//
//                        @Override
//                        public List<ACL> getAclForPath(String path) {
////                            return Arrays.asList(new ACL(ZooDefs.Perms.WRITE, new Id("digest", "sasl:zoo:crdwa")));
////                            return Arrays.asList(new ACL(ZooDefs.Perms.WRITE, new Id("digest", "sasl:zoo:crdwa")));
////                            return Arrays.asList(new ACL(ZooDefs.Perms.ADMIN, new Id("digest", generateDigest(user + ":" + pass))));
//                            return Arrays.asList(new ACL(ZooDefs.Perms.ADMIN, new Id("digest", user + ":" + generateDigest2(pass))));
//                        }
//                    })
                    ;
        };
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