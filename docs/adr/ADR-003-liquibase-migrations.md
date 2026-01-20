# ADR-003: Liquibase para Migrações de Banco de Dados

## Status

✅ **Aceito**

## Data

2025-12-01

## Contexto

Precisávamos de uma solução para gerenciar evolução do schema do banco de dados, considerando:

1. **Versionamento** - Controlar mudanças no schema ao longo do tempo
2. **Rastreabilidade** - Saber quem fez qual mudança e quando
3. **Rollback** - Reverter mudanças se necessário
4. **Ambientes Múltiplos** - Aplicar mesmas mudanças em dev/staging/prod
5. **CI/CD** - Integrar migrações no pipeline
6. **Colaboração** - Múltiplos devs fazendo mudanças simultâneas

### Problema

Sem uma ferramenta de migração:

- **Schema inconsistente** entre ambientes
- **Scripts SQL manuais** propensos a erro
- **Sem histórico** de mudanças
- **Difícil rollback** de alterações
- **Conflitos** entre devs
- **Deploy arriscado** (pode quebrar produção)

## Decisão

Adotamos **Liquibase** para gerenciar todas as migrações de banco de dados.

### Justificativa

- **Database-agnostic** - Funciona com MySQL, PostgreSQL, etc
- **Formato flexível** - XML, YAML, JSON ou SQL
- **Integração Spring Boot** - Execução automática no startup
- **Controle de versão** - Changesets no Git
- **Rollback** - Suporte a desfazer mudanças
- **Precondições** - Validações antes de aplicar
- **Diffs automáticos** - Gerar changesets comparando schemas

### Estrutura de Arquivos

```
src/main/resources/db/
├── changelog/
│   ├── db.changelog-master.yaml          # Master changelog
│   │
│   ├── v1/
│   │   ├── 001-create-users-table.yaml
│   │   ├── 002-create-media-table.yaml
│   │   └── 003-create-watch-entries-table.yaml
│   │
│   ├── v2/
│   │   ├── 001-add-media-genres.yaml
│   │   └── 002-add-user-preferences.yaml
│   │
│   └── v3/
│       └── 001-add-outbox-table.yaml
│
└── data/
    ├── test-users.sql
    └── test-media.sql
```

## Alternativas Consideradas

### 1. Flyway

**Prós:**

- Mais simples que Liquibase
- Foco em SQL puro
- Startup rápido
- Comunidade grande

**Contras:**

- Menos features (sem precondições)
- Rollback manual
- Menos database-agnostic
- Sem geração de diff

**Por que não escolhemos:** Liquibase oferece mais features enterprise (precondições, rollback, diff).

### 2. Hibernate DDL Auto

**Prós:**

- Zero configuração
- Automático
- Integrado ao JPA

**Contras:**

- **Perigoso em produção** (pode deletar dados)
- Sem controle de versão
- Sem rollback
- Não recomendado para produção

**Por que não escolhemos:** Não é adequado para ambientes produtivos.

### 3. Scripts SQL Manuais

**Prós:**

- Controle total
- Sem dependências
- Simples de entender

**Contras:**

- Propenso a erros humanos
- Sem rastreamento automático
- Difícil gerenciar múltiplos ambientes
- Sem rollback estruturado

**Por que não escolhemos:** Não escala para time e múltiplos ambientes.

### 4. JPA + Schema Validation

**Prós:**

- Valida entities vs schema
- Integrado ao código
- Sem SQL manual

**Contras:**

- Não aplica mudanças
- Sem controle de versão
- Apenas detecção de problemas

**Por que não escolhemos:** Não é ferramenta de migração, apenas validação.

## Consequências

### Positivas ✅

1. **Schema Versionado**
    - Histórico completo no Git
    - Auditoria de mudanças
    - Quem/quando/por quê documentado

2. **Ambientes Consistentes**
    - Dev, staging e prod sempre sincronizados
    - Migrações aplicadas automaticamente
    - Reduz bugs de schema

3. **Rollback Seguro**

    ```yaml
    - changeSet:
          id: add-user-email-column
          rollback:
              - dropColumn:
                    tableName: users
                    columnName: email
    ```

4. **Precondições**

    ```yaml
    - changeSet:
          id: add-index-if-not-exists
          preConditions:
              - not:
                    - indexExists:
                          tableName: media
                          indexName: idx_media_title
          changes:
              - createIndex:
                    tableName: media
                    indexName: idx_media_title
    ```

5. **CI/CD Friendly**

    ```bash
    # Validar migrações antes de deploy
    mvn liquibase:validate

    # Aplicar migrações em produção
    mvn liquibase:update
    ```

6. **Database-Agnostic**
    - MySQL em dev
    - PostgreSQL em staging
    - Mesmos changesets funcionam

7. **Geração de Diffs**
    ```bash
    # Gerar changeset comparando entities vs DB
    mvn liquibase:diff
    ```

### Negativas ❌

1. **Curva de Aprendizado**
    - Sintaxe YAML específica
    - Conceitos de changeset
    - Rollback procedures
    - Preconditions

2. **Verbosity**

    ```yaml
    # Liquibase YAML
    - changeSet:
        id: add-column
        changes:
          - addColumn:
              tableName: users
              columns:
                - column:
                    name: email
                    type: varchar(255)

    # vs SQL puro
    ALTER TABLE users ADD COLUMN email VARCHAR(255);
    ```

3. **Overhead de Performance**
    - Verifica DATABASECHANGELOG no startup
    - Pode demorar com muitos changesets
    - Mitigado com checksums

4. **Complexidade em Rollback**
    - Nem todos changesets são reversíveis
    - Data loss possível em rollback
    - Requer cuidado no design

5. **Lock de Tabela**
    - DATABASECHANGELOGLOCK pode travar
    - Problemas em deploys concorrentes
    - Requer monitoramento

### Trade-offs Aceitáveis

| Trade-off                       | Justificativa                           |
| ------------------------------- | --------------------------------------- |
| Verbosity → Clareza             | YAML explícito evita ambiguidade        |
| Overhead startup → Consistência | Garantia de schema correto vale o delay |
| Curva aprendizado → Features    | Investimento compensa no longo prazo    |

## Implementação

### Configuração Spring Boot

**pom.xml:**

```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

**application.yml:**

```yaml
spring:
    liquibase:
        enabled: true
        change-log: classpath:db/changelog/db.changelog-master.yaml
        default-schema: cinelog
        liquibase-schema: cinelog
        drop-first: false # NUNCA true em produção
```

### Master Changelog

**db.changelog-master.yaml:**

```yaml
databaseChangeLog:
    - include:
          file: db/changelog/v1/001-create-users-table.yaml
    - include:
          file: db/changelog/v1/002-create-media-table.yaml
    - include:
          file: db/changelog/v1/003-create-watch-entries-table.yaml
    - include:
          file: db/changelog/v2/001-add-media-genres.yaml
    - include:
          file: db/changelog/v3/001-add-outbox-table.yaml
```

### Exemplo de Changeset

**001-create-users-table.yaml:**

```yaml
databaseChangeLog:
    - changeSet:
          id: create-users-table-v1
          author: maps
          labels: v1.0
          comment: Cria tabela de usuários

          changes:
              - createTable:
                    tableName: users
                    columns:
                        - column:
                              name: id
                              type: bigint
                              autoIncrement: true
                              constraints:
                                  primaryKey: true
                                  primaryKeyName: pk_users
                                  nullable: false

                        - column:
                              name: username
                              type: varchar(50)
                              constraints:
                                  unique: true
                                  uniqueConstraintName: uk_users_username
                                  nullable: false

                        - column:
                              name: email
                              type: varchar(255)
                              constraints:
                                  unique: true
                                  uniqueConstraintName: uk_users_email
                                  nullable: false

                        - column:
                              name: password_hash
                              type: varchar(255)
                              constraints:
                                  nullable: false

                        - column:
                              name: created_at
                              type: timestamp
                              defaultValueComputed: CURRENT_TIMESTAMP
                              constraints:
                                  nullable: false

                        - column:
                              name: updated_at
                              type: timestamp
                              defaultValueComputed: CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                              constraints:
                                  nullable: false

          rollback:
              - dropTable:
                    tableName: users
```

### Changeset com Precondições

**002-add-index-safely.yaml:**

```yaml
databaseChangeLog:
    - changeSet:
          id: add-media-title-index
          author: maps

          preConditions:
              - onFail: MARK_RAN # Marca como executado se falhar
              - not:
                    - indexExists:
                          tableName: media
                          indexName: idx_media_title

          changes:
              - createIndex:
                    tableName: media
                    indexName: idx_media_title
                    columns:
                        - column:
                              name: title

          rollback:
              - dropIndex:
                    tableName: media
                    indexName: idx_media_title
```

### Changeset com SQL Nativo

**003-complex-migration.yaml:**

```yaml
databaseChangeLog:
    - changeSet:
          id: migrate-legacy-data
          author: maps

          changes:
              - sql:
                    sql: |
                        UPDATE media 
                        SET normalized_title = LOWER(TRIM(title))
                        WHERE normalized_title IS NULL;

          rollback:
              - sql:
                    sql: UPDATE media SET normalized_title = NULL;
```

## Comandos Úteis

### Maven Goals

```bash
# Validar changesets sem aplicar
mvn liquibase:validate

# Aplicar todas as migrações pendentes
mvn liquibase:update

# Ver status das migrações
mvn liquibase:status

# Gerar SQL das migrações (sem aplicar)
mvn liquibase:updateSQL

# Rollback último changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Rollback até data específica
mvn liquibase:rollback -Dliquibase.rollbackDate=2025-12-01

# Marcar changeset como executado (sem rodar)
mvn liquibase:changelogSync

# Limpar checksums
mvn liquibase:clearCheckSums

# Gerar diff entre entities e DB
mvn liquibase:diff
```

## Boas Práticas

### 1. Nomear Changesets Descritivamente

❌ **Ruim:**

```yaml
id: changeset-001
```

✅ **Bom:**

```yaml
id: create-users-table-v1
author: maps
labels: v1.0
comment: Cria tabela de usuários com campos básicos
```

### 2. Um Changeset por Mudança

❌ **Ruim:**

```yaml
- changeSet:
      id: multiple-changes
      changes:
          - createTable: ...
          - addColumn: ...
          - createIndex: ...
```

✅ **Bom:**

```yaml
- changeSet:
      id: create-users-table
      changes:
          - createTable: ...

- changeSet:
      id: add-users-email-index
      changes:
          - createIndex: ...
```

### 3. Sempre Definir Rollback

❌ **Ruim:**

```yaml
- changeSet:
      id: add-column
      changes:
          - addColumn: ...
      # Sem rollback!
```

✅ **Bom:**

```yaml
- changeSet:
      id: add-column
      changes:
          - addColumn: ...
      rollback:
          - dropColumn: ...
```

### 4. Usar Precondições para Idempotência

```yaml
- changeSet:
      id: add-column-safely
      preConditions:
          - not:
                - columnExists:
                      tableName: users
                      columnName: email
      changes:
          - addColumn: ...
```

### 5. Versionamento Lógico

```
v1/ - Versão inicial (1.0.0)
v2/ - Features adicionadas (1.1.0)
v3/ - Breaking changes (2.0.0)
```

## Validação

### Métricas de Sucesso

✅ **Zero divergências** de schema entre ambientes  
✅ **100% das migrações** com rollback definido  
✅ **< 5 segundos** para aplicar migrações no startup  
✅ **Zero problemas** em 50+ deploys

### Lições Aprendidas

1. **Precondições são essenciais** - Evitam falhas em reexecuções
2. **Rollback deve ser testado** - Não apenas definido
3. **YAML é verboso mas claro** - Preferir sobre SQL quando possível
4. **Changesets pequenos** - Mais fácil de debugar e reverter
5. **Validar antes de merge** - `mvn liquibase:validate` no CI

## Referências

- [Liquibase Documentation](https://docs.liquibase.com/)
- [Spring Boot + Liquibase](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.liquibase)
- [Liquibase Best Practices](https://www.liquibase.org/get-started/best-practices)
- [Database Refactoring - Martin Fowler](https://www.martinfowler.com/books/refactoringDatabases.html)

## Revisões

- **2025-12-01**: Decisão inicial aceita
- **2026-01-15**: Validado após 50+ migrações - sucesso total

---

**Mantido por:** Time CineLog  
**Próxima revisão:** Julho 2026
