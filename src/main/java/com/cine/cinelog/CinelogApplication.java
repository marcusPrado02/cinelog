package com.cine.cinelog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.cine.cinelog.shared.config.tmdb.TmdbProperties;

@SpringBootApplication(scanBasePackages = "com.cine.cinelog")
/**
 * Classe de configuração Spring para gerenciamento de cinelogapplication.
 *
 * <p>
 * Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.
 * </p>
 *
 * @since 1.0
 */
@EnableConfigurationProperties(TmdbProperties.class)
@EnableCaching
@EnableScheduling
public class CinelogApplication {

	public static void main(String[] args) {
		SpringApplication.run(CinelogApplication.class, args);
	}

}
