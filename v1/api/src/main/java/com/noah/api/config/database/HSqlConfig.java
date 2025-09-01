package com.noah.api.config.database;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@Slf4j
@Configuration
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@EnableJpaRepositories(entityManagerFactoryRef = "hSqlEntityManagerFactory", transactionManagerRef = "hSqlTransactionManager")
public class HSqlConfig {

	@Value("${spring.datasource.hSql.driver-class-name}")
	private String dbClassName;
	@Value("${spring.datasource.hSql.url}")
	private String dbUrl;
	@Value("${spring.datasource.hSql.username}")
	private String dbUser;
	@Value("${spring.datasource.hSql.password}")
	private String dbPassword;

	@Primary
	@Bean(name = "hSqlDataSource")
	public DataSource dataSource() throws SQLException {		
		DataSource realDataSource = DataSourceBuilder.create()
	            .driverClassName(dbClassName)
	            .url(dbUrl)
	            .username(dbUser)
	            .password(dbPassword)
	            .build();

	    log.info("hSqlDataSource...called");

	    // ProxyDataSource로 감싸기
	    DataSource dataSource = ProxyDataSourceBuilder
	            .create(realDataSource)
	            .name("HSQL-DS")
	            .listener(new SimpleQueryLoggingListener())  // 👈 커스텀 리스너 적용
	            .build();

	    // schema.sql 실행
	    runSchemaSql(realDataSource); // 주의: 여기서는 실제 datasource 사용해야 함

	    return dataSource;
	}

	@Primary
	@Bean(name = "hSqlEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean hSqlEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("hSqlDataSource") DataSource dataSource) {		
		log.info("hSqlEntityManagerFactory...called");
		return builder.dataSource(dataSource).packages("com.noah.api").persistenceUnit("primary").build();
	}

	@Primary
	@Bean(name = "hSqlTransactionManager")
	public PlatformTransactionManager hSqlTransactionManager(@Qualifier("hSqlEntityManagerFactory") EntityManagerFactory entityManagerFactory) {		
		log.info("hSqlTransactionManager...called");
		return new JpaTransactionManager(entityManagerFactory);
	}
	
	// schema.sql 파일을 실행하는 메서드
    private void runSchemaSql(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
        	log.info("runSchemaSql: schema.sql...called");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
            log.info("runSchemaSql: data.sql...called");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
        }
    }
}
