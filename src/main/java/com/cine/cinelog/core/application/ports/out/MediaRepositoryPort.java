package com.cine.cinelog.core.application.ports.out;

import java.util.List;
import java.util.Optional;

import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;

/**
 * Porta de saída para persistência de `Media`.
 *
 * Implementações desta interface (adapters) são responsáveis por salvar,
 * recuperar e pesquisar entidades de mídia em um armazenamento (banco de
 * dados, serviço externo, etc). A camada de domínio depende desta porta
 * para permanecer desacoplada da tecnologia de persistência.
 */
public interface MediaRepositoryPort {
    Media save(Media media);

    Optional<Media> findById(Long id);

    void deleteById(Long id);

    List<Media> find(MediaType type, String q, int page, int size);
}
