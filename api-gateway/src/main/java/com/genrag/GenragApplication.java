package com.genrag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

// TODO: remove the DataSourceAutoConfiguration exclude once the Postgres DB is configured.
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class GenragApplication {

	public static void main(String[] args) {
		SpringApplication.run(GenragApplication.class, args);
	}

}
