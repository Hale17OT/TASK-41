package com.dispatchops.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;

@Configuration
@ComponentScan(basePackages = {
    "com.dispatchops.application",
    "com.dispatchops.domain",
    "com.dispatchops.infrastructure"
})
@MapperScan("com.dispatchops.infrastructure.persistence.mapper")
@EnableTransactionManagement
@EnableScheduling
@PropertySource("classpath:application.properties")
public class AppConfig {
    @Value("${jdbc.url}") private String jdbcUrl;
    @Value("${jdbc.username}") private String jdbcUsername;
    @Value("${jdbc.password}") private String jdbcPassword;
    @Value("${security.aes.key}") private String aesKey;
    @Value("${security.hmac.key}") private String hmacKey;

    @jakarta.annotation.PostConstruct
    public void validateRequiredSecrets() {
        if (jdbcUrl == null || jdbcUrl.isBlank()) throw new IllegalStateException("JDBC_URL environment variable is required");
        if (jdbcUsername == null || jdbcUsername.isBlank()) throw new IllegalStateException("DB_USER environment variable is required");
        if (jdbcPassword == null || jdbcPassword.isBlank()) throw new IllegalStateException("DB_PASSWORD environment variable is required");
        if (aesKey == null || aesKey.isBlank() || aesKey.length() != 64) throw new IllegalStateException("AES_SECRET_KEY must be a 64-char hex string");
        if (hmacKey == null || hmacKey.isBlank() || hmacKey.length() != 64) throw new IllegalStateException("HMAC_SECRET_KEY must be a 64-char hex string");
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(jdbcUsername);
        config.setPassword(jdbcPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setInitializationFailTimeout(-1); // Don't fail on startup if DB isn't ready yet
        config.addDataSourceProperty("useSSL", "false");
        config.addDataSourceProperty("serverTimezone", "UTC");
        return new HikariDataSource(config);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfigLocation(new PathMatchingResourcePatternResolver().getResource("classpath:mybatis-config.xml"));
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mappers/*.xml"));
        factory.setTypeAliasesPackage("com.dispatchops.domain.model");
        return factory.getObject();
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
