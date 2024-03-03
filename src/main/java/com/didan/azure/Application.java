package com.didan.azure;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.Servers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@OpenAPIDefinition(
		info = @Info(
				title = "API DOCUMENTS FOR AZURE BLOB STORAGE",
				version = "1.0.0",
				description = "This api documents describe how to use our application",
				termsOfService = "Zdidane",
				license = @License(
						name = "Nguyen Di Dan",
						url = "https://znnguyenportfolio.netlify.app/"
				)
		),
		servers = {
				@Server(
						url = "http://localhost:8088/",
						description = "Url for localhost"
				)
		}
)
@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		bearerFormat = "JWT",
		scheme = "bearer"
)
public class Application {
	private static Logger logger = LoggerFactory.getLogger(Application.class);
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
		logger.info("Server is running");
	}

}