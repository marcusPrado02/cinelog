package com.cine.cinelog.shared.observability.audit;

import com.cine.cinelog.shared.security.IntegrityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * A08:2025 — Serviço de audit trail com hash chain (blockchain-lite).
 *
 * <p>
 * Cada registro de auditoria contém o hash do registro anterior,
 * formando uma cadeia imutável. Se qualquer registro for adulterado
 * no banco de dados (ex: DBA malicioso), a verificação da cadeia
 * detecta a inconsistência.
 * </p>
 *
 * <h3>Analogia:</h3>
 * 
 * <pre>
 * [GENESIS] ← Bloco 1 ← Bloco 2 ← Bloco 3 ← ... ← Bloco N
 *              │           │
 *              │           └── previousHash = hash(Bloco 1)
 *              └── previousHash = "GENESIS"
 * </pre>
 *
 * @since 1.3.0
 */
@Service
@Slf4j
public class AuditIntegrityService {

    private static final String GENESIS_HASH = "GENESIS";

    private final AuditIntegrityLogRepository repository;
    private final IntegrityService integrityService;

    public AuditIntegrityService(AuditIntegrityLogRepository repository,
            IntegrityService integrityService) {
        this.repository = repository;
        this.integrityService = integrityService;
    }

    /**
     * Registra uma operação com integridade na cadeia de auditoria.
     *
     * @param entityType  tipo da entidade (ex: "UserEntity")
     * @param entityId    ID da entidade
     * @param action      ação realizada ("CREATE", "UPDATE", "DELETE")
     * @param entityHmac  HMAC dos campos críticos da entidade
     * @param userId      ID do usuário que executou a ação (null se sistema)
     * @param description descrição legível da mudança
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String entityType, String entityId, String action,
            String entityHmac, Long userId, String description) {
        // Buscar hash do último registro da cadeia
        String previousHash = repository.findTopByOrderBySequenceNumberDesc()
                .map(AuditIntegrityLogEntity::getRecordHash)
                .orElse(GENESIS_HASH);

        Long sequenceNumber = repository.nextSequenceNumber();

        // Calcular hash deste registro (inclui previousHash → forma a cadeia)
        String recordHash = integrityService.sign(
                integrityService.buildSignableContent(
                        sequenceNumber, entityType, entityId, action, entityHmac, previousHash));

        AuditIntegrityLogEntity entry = new AuditIntegrityLogEntity();
        entry.setSequenceNumber(sequenceNumber);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setEntityHmac(entityHmac);
        entry.setPreviousHash(previousHash);
        entry.setRecordHash(recordHash);
        entry.setUserId(userId);
        entry.setCreatedAt(Instant.now());
        entry.setDescription(description);

        repository.save(entry);

        log.debug("A08:2025 — Audit chain: seq={}, entity={}#{}, action={}",
                sequenceNumber, entityType, entityId, action);
    }

    /**
     * Verifica a integridade de toda a cadeia de auditoria.
     *
     * @return resultado da verificação com detalhes
     */
    @Transactional(readOnly = true)
    public ChainVerificationResult verifyChain() {
        List<AuditIntegrityLogEntity> chain = repository.findAllByOrderBySequenceNumberAsc();

        if (chain.isEmpty()) {
            return new ChainVerificationResult(true, 0, "Cadeia vazia — OK");
        }

        String expectedPreviousHash = GENESIS_HASH;

        for (int i = 0; i < chain.size(); i++) {
            AuditIntegrityLogEntity entry = chain.get(i);

            // 1. Verificar se previousHash bate com o hash do registro anterior
            if (!entry.getPreviousHash().equals(expectedPreviousHash)) {
                String msg = String.format(
                        "Cadeia quebrada no registro seq=%d: previousHash esperado=%s, encontrado=%s",
                        entry.getSequenceNumber(), expectedPreviousHash, entry.getPreviousHash());
                log.error("A08:2025 — {}", msg);
                return new ChainVerificationResult(false, i, msg);
            }

            // 2. Recalcular recordHash e verificar
            String recalculatedHash = integrityService.sign(
                    integrityService.buildSignableContent(
                            entry.getSequenceNumber(), entry.getEntityType(),
                            entry.getEntityId(), entry.getAction(),
                            entry.getEntityHmac(), entry.getPreviousHash()));

            if (!integrityService.verify(
                    integrityService.buildSignableContent(
                            entry.getSequenceNumber(), entry.getEntityType(),
                            entry.getEntityId(), entry.getAction(),
                            entry.getEntityHmac(), entry.getPreviousHash()),
                    entry.getRecordHash())) {
                String msg = String.format(
                        "Hash adulterado no registro seq=%d: recordHash não corresponde",
                        entry.getSequenceNumber());
                log.error("A08:2025 — {}", msg);
                return new ChainVerificationResult(false, i, msg);
            }

            expectedPreviousHash = entry.getRecordHash();
        }

        log.info("A08:2025 — Cadeia de auditoria íntegra: {} registros verificados", chain.size());
        return new ChainVerificationResult(true, chain.size(), "Cadeia íntegra");
    }

    /**
     * Resultado da verificação da cadeia de auditoria.
     */
    public record ChainVerificationResult(
            boolean valid,
            int recordsVerified,
            String message) {
    }
}
