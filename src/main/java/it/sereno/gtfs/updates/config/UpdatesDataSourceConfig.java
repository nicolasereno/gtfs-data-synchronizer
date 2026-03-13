package it.sereno.gtfs.updates.config;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.dialect.H2Dialect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import java.util.Map;

@Configuration
@EnableJpaRepositories(
		basePackages = "it.sereno.gtfs.updates",
		entityManagerFactoryRef = "updatesEntityManagerFactory",
		transactionManagerRef = "updatesTransactionManager"
)
public class UpdatesDataSourceConfig {

	@Bean
	@ConfigurationProperties( "spring.datasource.jdbc.updates" )
	public DataSource updatesDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean
	public JdbcTemplate updatesJdbcTemplate( @Qualifier( "updatesDataSource" ) DataSource jdbcDataSource ) {
		return new JdbcTemplate( jdbcDataSource );
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean updatesEntityManagerFactory(
			EntityManagerFactoryBuilder builder,
			@Qualifier( "updatesDataSource" ) DataSource updatesDataSource
	) {
		return builder
				.dataSource( updatesDataSource )
				.packages( "it.sereno.gtfs.updates" )
				.persistenceUnit( "updates" )
				.properties( Map.of("hibernate.dialect", H2Dialect.class.getName()) )
				.build();
	}

	@Bean
	public PlatformTransactionManager updatesTransactionManager(
			@Qualifier( "updatesEntityManagerFactory" ) EntityManagerFactory emf
	) {
		return new JpaTransactionManager( emf );
	}
}
