package eus.ibai.family.recipes.food.wm.infrastructure.zookeeper;

import lombok.NonNull;
import org.testcontainers.containers.GenericContainer;

public class ZookeeperContainer<SELF extends ZookeeperContainer<SELF>> extends GenericContainer<SELF> {

    private static final int ZOOKEEPER_PORT = 2181;

    public ZookeeperContainer(@NonNull String dockerImageName) {
        super(dockerImageName);
        addExposedPort(ZOOKEEPER_PORT);
    }

    @Override
    protected void configure() {
        addEnv("ALLOW_ANONYMOUS_LOGIN", "yes");
        addEnv("ZOO_SERVER_ID", "1");
        addEnv("ZOO_SERVERS", "0.0.0.0:2888:3888");
    }

    public int getMappedHttpPort() {
        return getMappedPort(ZOOKEEPER_PORT);
    }
}
