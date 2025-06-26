package ai.shreds.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:20000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:300000}")
    private long idleTimeout;

    @Value("${spring.r2dbc.url:r2dbc:postgresql://localhost:5432/orders}")
    private String r2dbcUrl;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(datasourceUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setLeakDetectionThreshold(60000);
        config.setPoolName("OrderFulfillmentPool");
        
        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        return new HikariDataSource(config);
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        // Extract connection details from R2DBC URL
        String parsedHost = extractHostFromR2dbcUrl(r2dbcUrl);
        int parsedPort = extractPortFromR2dbcUrl(r2dbcUrl);
        String parsedDatabase = extractDatabaseFromR2dbcUrl(r2dbcUrl);
        
        return new PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(parsedHost)
                .port(parsedPort)
                .database(parsedDatabase)
                .username(username)
                .password(password)
                .build()
        );
    }

    private String extractHostFromR2dbcUrl(String url) {
        // Extract host from r2dbc:postgresql://localhost:5432/orders
        String withoutProtocol = url.substring(url.indexOf("://") + 3);
        String hostPart = withoutProtocol.substring(0, withoutProtocol.indexOf(":"));
        return hostPart.isEmpty() ? "localhost" : hostPart;
    }

    private int extractPortFromR2dbcUrl(String url) {
        try {
            String withoutProtocol = url.substring(url.indexOf("://") + 3);
            String portPart = withoutProtocol.substring(
                withoutProtocol.indexOf(":") + 1, 
                withoutProtocol.indexOf("/")
            );
            return Integer.parseInt(portPart);
        } catch (Exception e) {
            return 5432; // default PostgreSQL port
        }
    }

    private String extractDatabaseFromR2dbcUrl(String url) {
        String withoutProtocol = url.substring(url.indexOf("://") + 3);
        String dbPart = withoutProtocol.substring(withoutProtocol.indexOf("/") + 1);
        return dbPart.isEmpty() ? "orders" : dbPart;
    }
}