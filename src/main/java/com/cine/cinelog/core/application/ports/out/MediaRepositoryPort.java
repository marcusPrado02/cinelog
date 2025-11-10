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
 * dados, serviço externo, etc).
 */
public interface MediaRepositoryPort {

    /**
     * Salva uma mídia no repositório.
     *
     * @param media O objeto Media a ser salvo.
     * @return A mídia salva.
     */
    Media save(Media media);

    /**
     * Recupera uma mídia existente do repositório.
     *
     * @param id O ID da mídia a ser recuperada.
     * @return Um Optional contendo a mídia encontrada, ou vazio se não
     *         encontrado.
     */
    Optional<Media> findById(Long id);

    /**
     * Recupera todas as mídias existentes do repositório.
     *
     * @return Uma lista de mídias encontradas.
     */
    void deleteById(Long id);

    /**
     * Pesquisa mídias com base em critérios fornecidos.
     *
     * @param type O tipo de mídia a ser pesquisado.
     * @param q    A consulta de pesquisa.
     * @param page O número da página para a pesquisa.
     * @param size O tamanho da página (número de itens por página).
     * @return Uma lista de mídias que correspondem aos critérios de pesquisa.
     */
    List<Media> find(MediaType type, String q, int page, int size);
}
