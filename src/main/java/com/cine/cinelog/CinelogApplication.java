package com.cine.cinelog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cine.cinelog")
public class CinelogApplication {

	/**
	 * Ponto de entrada da aplicação Spring Boot.
	 *
	 * A classe principal não deve conter lógica de negócio. Mantém-se mínima
	 * para permitir inicialização da aplicação e descoberta automática do
	 * contexto Spring.
	 */

	public static void main(String[] args) {
		SpringApplication.run(CinelogApplication.class, args);
	}

}
