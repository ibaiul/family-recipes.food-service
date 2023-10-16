package eus.ibai.family.recipes.food.wm.infrastructure.zookeeper;

import lombok.NonNull;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import java.io.File;

public class ZookeeperContainer<SELF extends ZookeeperContainer<SELF>> extends GenericContainer<SELF> {

    private static final int ZOOKEEPER_PORT = 2181;
    private static final int ZOOKEEPER_SSL_PORT = 3181;

    private boolean ssl = false;

    private File keystore;
    private String keystorePass;
    private File truststore;
    private String truststorePass;
    private boolean auth = false;

    public ZookeeperContainer(@NonNull String dockerImageName) {
        super(dockerImageName);
        addExposedPort(ZOOKEEPER_PORT);
    }

    public SELF withSsl(File keystore, String keystorePass, File truststore, String truststorePass) {
        this.ssl = true;
        this.keystore = keystore;
        this.keystorePass = keystorePass;
        this.truststore = truststore;
        this.truststorePass = truststorePass;
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
            addFileSystemBind(keystore.getAbsolutePath(), "/bitnami/zookeeper/certs/server-keystore.jks", BindMode.READ_WRITE);
            addFileSystemBind(truststore.getAbsolutePath(), "/bitnami/zookeeper/certs/server-truststore.jks", BindMode.READ_WRITE);
            addEnv("ZOO_TLS_CLIENT_ENABLE", "true");
            addEnv("ZOO_TLS_CLIENT_KEYSTORE_FILE", "/bitnami/zookeeper/certs/server-keystore.jks");
            addEnv("ZOO_TLS_CLIENT_KEYSTORE_PASSWORD", keystorePass);
            addEnv("ZOO_TLS_CLIENT_TRUSTSTORE_FILE", "/bitnami/zookeeper/certs/server-truststore.jks");
            addEnv("ZOO_TLS_CLIENT_TRUSTSTORE_PASSWORD", truststorePass);
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
}
