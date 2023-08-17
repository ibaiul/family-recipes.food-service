package eus.ibai.family.recipes.food.test;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TestData {

    public static final String COMPONENT_NAME = "component";

    public static void mockConnectionFactoryHealthUp(ConnectionFactory connectionFactory) {
        ConnectionFactoryMetadata mockConnectionFactoryMetadata = mock(ConnectionFactoryMetadata.class);
        when(mockConnectionFactoryMetadata.getName()).thenReturn("name");
        when(connectionFactory.getMetadata()).thenReturn(mockConnectionFactoryMetadata);
        Connection mockConnection = mock(Connection.class);
        doReturn(Mono.just(mockConnection)).when(connectionFactory).create();
        when(mockConnection.close()).thenReturn(Mono.empty());
        when(mockConnection.validate(any())).thenReturn(Mono.just(true));
    }

    public static void mockDatasourceHealthUp(DataSource dataSource) throws SQLException {
        DatabaseMetaData mockDatabaseMetaData = mock(DatabaseMetaData.class);
        when(mockDatabaseMetaData.getDatabaseProductName()).thenReturn("name");
        java.sql.Connection mockConnection = mock(java.sql.Connection.class);
        when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetaData);
        when(mockConnection.isValid(0)).thenReturn(true);
        when(dataSource.getConnection()).thenReturn(mockConnection);
    }
}
