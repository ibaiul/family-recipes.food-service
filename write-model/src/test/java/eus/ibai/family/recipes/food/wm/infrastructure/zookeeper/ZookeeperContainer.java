package eus.ibai.family.recipes.food.wm.infrastructure.zookeeper;

import eus.ibai.family.recipes.food.wm.test.ZookeeperUtils;
import lombok.NonNull;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class ZookeeperContainer<SELF extends ZookeeperContainer<SELF>> extends GenericContainer<SELF> {

    private static final int ZOOKEEPER_PORT = 2181;

    private static final int ZOOKEEPER_SSL_PORT = 3181;

    public static final String KEYSTORE_CONTAINER_PATH = "/bitnami/zookeeper/certs/server-keystore.jks";

    public static final String TRUSTSTORE_CONTAINER_PATH = "/bitnami/zookeeper/certs/server-truststore.jks";

    private boolean ssl = false;

    private File clientProperties;

    private boolean auth = false;

    public ZookeeperContainer(@NonNull String dockerImageName) {
        super(dockerImageName);
        addExposedPort(ZOOKEEPER_PORT);
    }

    public SELF withSsl() {
        this.ssl = true;
        return self();
    }

    public SELF withAuth() {
        this.auth = true;
        return self();
    }

    @Override
    protected void configure() {
        addEnv("ZOO_SERVER_ID", "1");
        addEnv("ZOO_SERVERS", "0.0.0.0:2888:3888");
        if (ssl) {
            addExposedPort(ZOOKEEPER_SSL_PORT);

            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-r--r--");
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
            File serverKeystore;
            File serverTruststore;
            try {
                serverKeystore = Files.createTempFile("server-keystore-", ".jks", attr).toFile();
                serverKeystore.deleteOnExit();
                serverTruststore = Files.createTempFile("server-truststore-", ".jks", attr).toFile();
                serverTruststore.deleteOnExit();
                File clientKeystore = Files.createTempFile("client-keystore-", ".jks").toFile();
                clientKeystore.deleteOnExit();
                File clientTruststore = Files.createTempFile("client-truststore-", ".jks").toFile();
                clientTruststore.deleteOnExit();
                ZookeeperUtils.createJksFiles(serverKeystore, serverTruststore, clientKeystore, clientTruststore);

                clientProperties = Files.createTempFile("client", ".properties").toFile();
                ZookeeperUtils.createClientProperties(clientProperties, clientKeystore, clientTruststore);
                clientProperties.deleteOnExit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            addFileSystemBind(serverKeystore.getAbsolutePath(), KEYSTORE_CONTAINER_PATH, BindMode.READ_WRITE);
            addFileSystemBind(serverTruststore.getAbsolutePath(), TRUSTSTORE_CONTAINER_PATH, BindMode.READ_WRITE);
            addEnv("ZOO_TLS_CLIENT_ENABLE", "true");
            addEnv("ZOO_TLS_CLIENT_KEYSTORE_FILE", KEYSTORE_CONTAINER_PATH);
            addEnv("ZOO_TLS_CLIENT_KEYSTORE_PASSWORD", "123456");
            addEnv("ZOO_TLS_CLIENT_TRUSTSTORE_FILE", TRUSTSTORE_CONTAINER_PATH);
            addEnv("ZOO_TLS_CLIENT_TRUSTSTORE_PASSWORD", "123456");
        }
        if (auth) {
            addEnv("ZOO_ENABLE_AUTH", "yes");
            addEnv("ZOO_TLS_CLIENT_AUTH", "need");
        } else {
            addEnv("ALLOW_ANONYMOUS_LOGIN", "yes");
        }
    }

    public int getHttpPort() {
        return getMappedPort(ZOOKEEPER_PORT);
    }

    public int getHttpsPort() {
        return getMappedPort(ZOOKEEPER_SSL_PORT);
    }

    public File getClientProperties() {
        return clientProperties;
    }
}
