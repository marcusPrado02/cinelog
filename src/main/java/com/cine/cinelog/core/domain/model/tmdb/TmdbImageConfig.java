package com.cine.cinelog.core.domain.model.tmdb;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuração de imagens do TMDb, retornada por GET /configuration.
 *
 * Usada para montar URLs de posters, backdrops e perfis de forma dinâmica.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbImageConfig {

    private String baseUrl;

    private String secureBaseUrl;

    private List<String> posterSizes;

    private List<String> backdropSizes;

    private List<String> profileSizes;

    /**
     * Retorna uma URL absoluta para um poster, dada a path (ex.: "/abc123.jpg")
     * e um tamanho (ex.: "w500"). Caso algum parâmetro esteja inválido, retorna
     * null.
     */
    public String buildPosterUrl(String size, String path) {
        if (secureBaseUrl == null || size == null || path == null || path.isBlank()) {
            return null;
        }
        return secureBaseUrl + size + path;
    }

    /**
     * Similar ao buildPosterUrl, porém para backdrops.
     */
    public String buildBackdropUrl(String size, String path) {
        if (secureBaseUrl == null || size == null || path == null || path.isBlank()) {
            return null;
        }
        return secureBaseUrl + size + path;
    }

    /**
     * Similar ao buildPosterUrl, porém para fotos de perfil.
     */
    public String buildProfileUrl(String size, String path) {
        if (secureBaseUrl == null || size == null || path == null || path.isBlank()) {
            return null;
        }
        return secureBaseUrl + size + path;
    }
}
