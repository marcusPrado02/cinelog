package com.cine.cinelog.core.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;

import com.cine.cinelog.core.domain.state.WatchEntryStatus;
import com.cine.cinelog.core.domain.state.WatchEntryStatusFactory;

import lombok.Getter;
import lombok.Setter;

@Getter
/**
 * Representa WatchEntry no domínio do sistema.
 *
 * <p>
 * Esta classe encapsula os conceitos e regras de negócio relacionados a
 * watchentry.
 * Contém a lógica de domínio pura, independente de frameworks e infraestrutura.
 * </p>
 *
 * @since 1.0
 */
@Setter
public class WatchEntry extends Auditable {
    private Long id;
    private Long userId;
    private Long mediaId;
    private Long episodeId;
    private BigDecimal rating;
    private String comment;
    private LocalDate watchedAt;

    /**
     * Estado atual do WatchEntry (State Pattern).
     * Não persistido diretamente - veja statusType.
     * Reconstruído via WatchEntryStatusFactory após load do banco.
     */
    private transient WatchEntryStatus status;

    /**
     * Tipo do estado para persistência.
     * Sincronizado automaticamente com status.
     */
    private WatchEntryStatusType statusType;

    public WatchEntry(Long id, Long userId, Long mediaId, Long episodeId,
            BigDecimal rating, String comment, LocalDate watchedAt, LocalDateTime createdAt, LocalDateTime updatedAt,
            Long createdBy, Long updatedBy, Long version) {

        if (userId == null)
            throw new IllegalArgumentException("userId is required");
        if (mediaId == null && episodeId == null)
            throw new IllegalArgumentException("Either mediaId or episodeId must be provided");
        if (rating != null && (rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(BigDecimal.TEN) > 0))
            throw new IllegalArgumentException("rating must be 0..10");

        this.id = id;
        this.userId = userId;
        this.mediaId = mediaId;
        this.episodeId = episodeId;
        this.rating = rating;
        this.comment = comment;
        this.watchedAt = watchedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.version = version;

        // Inicializa com estado PLANNING (padrão para novas entradas)
        this.status = WatchEntryStatusFactory.createInitial();
        this.statusType = WatchEntryStatusType.PLANNING;
    }

    public WatchEntry withId(Long id) {
        return new WatchEntry(id, userId, mediaId, episodeId, rating, comment, watchedAt, createdAt, updatedAt,
                createdBy, updatedBy, version);
    }

    /**
     * Setter customizado para rating com validação de estado.
     *
     * @param rating nova nota (0-10 ou null)
     */
    public void setRating(BigDecimal rating) {
        if (this.status != null) {
            this.status.validateRating(rating);
        }
        this.rating = rating;
    }

    /**
     * Setter customizado para comment com validação de estado.
     *
     * @param comment novo comentário
     */
    public void setComment(String comment) {
        if (this.status != null) {
            this.status.validateComment(comment);
        }
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WatchEntry we && Objects.equals(id, we.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void applyRating(BigDecimal rating, String comment) {
        // Valida rating através do estado atual
        if (this.status != null) {
            this.status.validateRating(rating);
        }

        this.setRating(rating);
        if (comment != null && !comment.isBlank()) {
            this.setComment(comment.trim());
        }
        this.setUpdatedAt(LocalDateTime.now());
    }

    public WatchEntry updateFrom(WatchEntry patch) {
        if (patch == null)
            return this;

        this.mediaId = (patch.mediaId != null) ? patch.mediaId : this.mediaId;
        this.episodeId = (patch.episodeId != null) ? patch.episodeId : this.episodeId;
        this.rating = (patch.rating != null) ? patch.rating : this.rating;
        this.comment = (patch.comment != null) ? patch.comment : this.comment;
        this.watchedAt = (patch.watchedAt != null) ? patch.watchedAt : this.watchedAt;
        return this;
    }

    // ==================== State Pattern Methods ====================

    /**
     * Reconstrói o estado após load do banco de dados.
     * Deve ser chamado automaticamente pela camada de persistência.
     */
    public void reconstructState() {
        if (this.statusType == null) {
            this.statusType = WatchEntryStatusType.PLANNING;
        }
        this.status = WatchEntryStatusFactory.create(this.statusType);
    }

    /**
     * Sincroniza statusType com o estado atual.
     * Deve ser chamado antes de persistir no banco.
     */
    public void syncStatusType() {
        if (this.status != null) {
            this.statusType = this.status.getType();
        }
    }

    /**
     * Inicia assistir - Transição: PLANNING → WATCHING
     *
     * @throws com.cine.cinelog.core.domain.error.DomainException se transição
     *                                                            inválida
     */
    public void startWatching() {
        this.status = this.status.startWatching(this);
        this.statusType = this.status.getType();
        this.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Marca como completo - Transição: WATCHING → COMPLETED
     *
     * @param finalRating nota final (0-10), pode ser null
     * @throws com.cine.cinelog.core.domain.error.DomainException se transição
     *                                                            inválida
     */
    public void markAsCompleted(BigDecimal finalRating) {
        this.status = this.status.complete(this, finalRating);
        this.statusType = this.status.getType();
        this.watchedAt = LocalDate.now();
        this.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Abandona - Transição: PLANNING|WATCHING → DROPPED
     *
     * @param reason motivo da desistência (recomendado)
     * @throws com.cine.cinelog.core.domain.error.DomainException se transição
     *                                                            inválida
     */
    public void abandon(String reason) {
        this.status = this.status.drop(this, reason);
        this.statusType = this.status.getType();
        this.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Reavalia (apenas para COMPLETED).
     * Permite atualizar rating sem mudar de estado.
     *
     * @param newRating nova nota (0-10)
     * @throws com.cine.cinelog.core.domain.error.DomainException se estado não
     *                                                            permite
     */
    public void updateRating(BigDecimal newRating) {
        this.status.validateRating(newRating);
        this.rating = newRating;
        this.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Verifica se entrada pode ser modificada.
     * Estados finais (COMPLETED, DROPPED) não permitem mudanças de estado.
     *
     * @return true se não for estado final
     */
    public boolean isModifiable() {
        return this.status != null && !this.status.isFinal();
    }

    /**
     * Obtém descrição amigável do estado atual.
     *
     * @return descrição em português (ex: "Planejando assistir")
     */
    public String getStatusDescription() {
        return this.status != null ? this.status.getDescription() : "Estado desconhecido";
    }

}
