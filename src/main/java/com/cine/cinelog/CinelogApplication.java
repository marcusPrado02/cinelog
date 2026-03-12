package com.cine.cinelog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

import com.cine.cinelog.features.batch.config.BatchJobProperties;
import com.cine.cinelog.features.reports.config.ReportProperties;
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
@EnableConfigurationProperties({ TmdbProperties.class, BatchJobProperties.class, ReportProperties.class })
@EnableCaching
@EnableAsync
public class CinelogApplication {

	public static void main(String[] args) {
		SpringApplication.run(CinelogApplication.class, args);
	}

}
