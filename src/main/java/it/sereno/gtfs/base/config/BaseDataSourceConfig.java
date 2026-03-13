package it.sereno.gtfs.base.config;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.dialect.PostgreSQLDialect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import java.util.Map;

@Configuration
@EnableJpaRepositories(
		basePackages = "it.sereno.gtfs.base",
		entityManagerFactoryRef = "baseEntityManagerFactory",
		transactionManagerRef = "baseTransactionManager"
)
public class BaseDataSourceConfig {

	@Bean
	@Primary
	@ConfigurationProperties( "spring.datasource.jdbc.base" )
	public DataSource baseDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean
	@Primary
	public JdbcTemplate baseJdbcTemplate( @Qualifier( "baseDataSource" ) DataSource jdbcDataSource ) {
		return new JdbcTemplate( jdbcDataSource );
	}

	@Primary
	@Bean
	public LocalContainerEntityManagerFactoryBean baseEntityManagerFactory(
			EntityManagerFactoryBuilder builder,
			@Qualifier( "baseDataSource" ) DataSource baseDataSource
	) {
		return builder
				.dataSource( baseDataSource )
				.packages( "it.sereno.gtfs.base" )
				.persistenceUnit( "base" )
				.properties( Map.of( "hibernate.dialect", PostgreSQLDialect.class.getName() ) )
				.build();
	}

	@Primary
	@Bean
	public PlatformTransactionManager baseTransactionManager(
			@Qualifier( "baseEntityManagerFactory" ) EntityManagerFactory emf
	) {
		return new JpaTransactionManager( emf );
	}
}
