package com.cine.cinelog.shared.config.tmdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de configuração para integração com a API do TMDb.
 *
 * As propriedades são lidas do arquivo application.yml (prefixo
 * "cinelog.tmdb").
 */
@ConfigurationProperties(prefix = "cinelog.tmdb")
public class TmdbProperties {

    /**
     * Base URL da API v3 do TMDb.
     * Exemplo: https://api.themoviedb.org/3
     */
    private String baseUrl = "https://api.themoviedb.org/3";

    /**
     * Token de acesso (API Read Access Token v4) para autenticação via Bearer.
     */
    private String bearerToken;

    /**
     * Idioma padrão para as respostas, ex.: "pt-BR".
     */
    private String language = "pt-BR";

    /**
     * Região padrão (usada em algumas buscas), ex.: "BR".
     */
    private String region = "BR";

    /**
     * Timeout em milissegundos para requisições HTTP ao TMDb.
     */
    private int timeoutMs = 3000;

    // ================== getters/setters ==================

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
