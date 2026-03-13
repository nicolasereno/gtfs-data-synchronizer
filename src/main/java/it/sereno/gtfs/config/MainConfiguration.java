package it.sereno.gtfs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MainConfiguration {

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}

