package eus.ibai.family.recipes.food.rm.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class FlywayConfig {

    private static final String VENDOR_PLACEHOLDER = "{vendor}";

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(@Qualifier("event-store-db") DataSource commandDatasource,
                                                           @Value("${spring.datasource.url}") String commandDbUrl,
                                                           @Value("${spring.flyway.locations-command-db}") String commandDbLocations) {
        return flyway -> {
            log.info("Applying projections database Flyway migrations.");
            flyway.migrate();

            log.info("Applying event store Flyway migrations.");
            String vendor = DatabaseDriver.fromJdbcUrl(commandDbUrl).getId();
            String[] locations = commandDbLocations.replace(VENDOR_PLACEHOLDER, vendor).split(",");
            Flyway.configure()
                    .dataSource(commandDatasource)
                    .locations(locations)
                    .load()
                    .migrate();
        };
    }
}
