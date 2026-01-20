# 🤝 Guia de Contribuição - CineLog

Obrigado por considerar contribuir para o CineLog! Este guia fornece diretrizes para contribuições ao projeto.

## 📋 Índice

1. [Código de Conduta](#código-de-conduta)
2. [Como Contribuir](#como-contribuir)
3. [Processo de Desenvolvimento](#processo-de-desenvolvimento)
4. [Padrões de Commit](#padrões-de-commit)
5. [Pull Requests](#pull-requests)
6. [Reportando Bugs](#reportando-bugs)
7. [Sugerindo Melhorias](#sugerindo-melhorias)

---

## Código de Conduta

### Nosso Compromisso

Estamos comprometidos em fornecer uma experiência acolhedora e inspiradora para todos. Não toleramos assédio ou comportamento inadequado.

### Comportamentos Esperados

-   ✅ Seja respeitoso e profissional
-   ✅ Aceite críticas construtivas
-   ✅ Foque no que é melhor para a comunidade
-   ✅ Mostre empatia com outros membros

### Comportamentos Inaceitáveis

-   ❌ Linguagem ofensiva ou discriminatória
-   ❌ Assédio público ou privado
-   ❌ Publicação de informações privadas de terceiros
-   ❌ Conduta não profissional

---

## Como Contribuir

### Tipos de Contribuição

Valorizamos todos os tipos de contribuição:

1. **Código**

    - Novas features
    - Correções de bugs
    - Melhorias de performance
    - Refatorações

2. **Documentação**

    - Corrigir typos
    - Adicionar exemplos
    - Traduzir documentação
    - Melhorar clareza

3. **Testes**

    - Adicionar testes unitários
    - Adicionar testes de integração
    - Melhorar cobertura

4. **Feedback**
    - Reportar bugs
    - Sugerir features
    - Compartilhar casos de uso

---

## Processo de Desenvolvimento

### 1. Fork e Clone

```bash
# Fork o repositório no GitHub
# Clone seu fork
git clone https://github.com/SEU_USUARIO/cinelog.git
cd cinelog

# Adicione o repositório original como remote
git remote add upstream https://github.com/marcusPrado02/cinelog.git
```

### 2. Crie uma Branch

```bash
# Atualize seu fork
git checkout master
git pull upstream master

# Crie uma branch para sua feature/fix
git checkout -b feature/nome-da-feature
# ou
git checkout -b fix/descricao-do-bug
```

### Convenção de Nomes de Branches

-   `feature/` - Nova funcionalidade
-   `fix/` - Correção de bug
-   `docs/` - Alterações em documentação
-   `refactor/` - Refatoração de código
-   `test/` - Adição/melhoria de testes
-   `perf/` - Melhorias de performance

**Exemplos**:

```bash
feature/add-review-system
fix/null-pointer-in-media-service
docs/update-api-guide
refactor/simplify-mapper-logic
test/add-user-controller-tests
```

### 3. Desenvolva

```bash
# Faça suas alterações
# Execute os testes
./mvnw test

# Verifique a cobertura
./mvnw verify

# Verifique o código (Checkstyle, PMD, SpotBugs)
./mvnw validate
```

### 4. Commit

Siga o padrão [Conventional Commits](https://www.conventionalcommits.org/):

```bash
git add .
git commit -m "feat: adiciona sistema de reviews"
```

### 5. Push e Pull Request

```bash
# Push para seu fork
git push origin feature/nome-da-feature

# Abra um Pull Request no GitHub
```

---

## Padrões de Commit

### Formato

```
<tipo>(<escopo>): <descrição>

[corpo opcional]

[rodapé opcional]
```

### Tipos

-   `feat` - Nova funcionalidade
-   `fix` - Correção de bug
-   `docs` - Documentação
-   `style` - Formatação (sem mudança de código)
-   `refactor` - Refatoração
-   `test` - Testes
-   `chore` - Manutenção (build, deps, etc)
-   `perf` - Performance

### Exemplos

#### Feature

```
feat(media): adiciona busca por título

Implementa endpoint GET /api/v1/media/search?title={title}
com paginação e ordenação.

Closes #123
```

#### Fix

```
fix(auth): corrige expiração do token JWT

O token agora expira corretamente após 1 hora.

Fixes #456
```

#### Docs

```
docs(api): atualiza exemplos de autenticação

Adiciona exemplos de uso do refresh token.
```

#### Refactor

```
refactor(mapper): simplifica conversão de DTOs

Remove código duplicado e melhora legibilidade.
```

### Regras

1. **Tipo é obrigatório**
2. **Escopo é opcional** (media, auth, user, etc)
3. **Descrição em minúsculas**
4. **Sem ponto final na descrição**
5. **Corpo do commit explica o "porquê"**
6. **Referências issues quando aplicável** (`Closes #123`, `Fixes #456`)

---

## Pull Requests

### Checklist

Antes de abrir um PR, verifique:

-   [ ] Código segue os padrões do projeto
-   [ ] Testes passam (`./mvnw test`)
-   [ ] Cobertura mantida/melhorada
-   [ ] Documentação atualizada
-   [ ] Commits seguem convenção
-   [ ] Branch está atualizada com `master`
-   [ ] Sem conflitos de merge

### Template de PR

Ao abrir um PR, use este template:

```markdown
## Descrição

Breve descrição das alterações.

## Tipo de Mudança

-   [ ] Bug fix (non-breaking change)
-   [ ] Nova feature (non-breaking change)
-   [ ] Breaking change
-   [ ] Documentação

## Como Testar

1. Passo 1
2. Passo 2
3. Verificar resultado esperado

## Screenshots (se aplicável)

## Checklist

-   [ ] Testes unitários adicionados/atualizados
-   [ ] Testes de integração adicionados/atualizados
-   [ ] Documentação atualizada
-   [ ] Changelog atualizado (se aplicável)

## Issues Relacionadas

Closes #123
```

### Revisão de Código

Todos os PRs passam por code review:

1. **Automatic Checks** (CI/CD)

    - Build
    - Testes
    - Linting
    - Cobertura

2. **Manual Review**
    - Qualidade do código
    - Conformidade com padrões
    - Performance
    - Segurança

### Processo de Aprovação

-   PRs pequenos: 1 aprovação
-   PRs grandes: 2 aprovações
-   Breaking changes: 2+ aprovações + discussão

---

## Reportando Bugs

### Antes de Reportar

1. Verifique se o bug já foi reportado
2. Tente reproduzir em ambiente limpo
3. Colete informações relevantes

### Template de Bug Report

```markdown
**Descrição**
Descrição clara e concisa do bug.

**Passos para Reproduzir**

1. Vá para '...'
2. Click em '...'
3. Veja o erro

**Comportamento Esperado**
O que deveria acontecer.

**Comportamento Atual**
O que está acontecendo.

**Screenshots**
Se aplicável, adicione screenshots.

**Ambiente**

-   OS: [e.g. Ubuntu 22.04]
-   Java: [e.g. 21]
-   Versão do projeto: [e.g. 0.0.1-SNAPSHOT]

**Logs**
```

Cole logs relevantes aqui

```

**Contexto Adicional**
Qualquer informação adicional.
```

---

## Sugerindo Melhorias

### Template de Feature Request

```markdown
**Sua feature está relacionada a um problema?**
Descrição clara do problema. Ex: "Sempre fico frustrado quando [...]"

**Descreva a solução que você gostaria**
Descrição clara e concisa da solução.

**Descreva alternativas consideradas**
Outras soluções ou features consideradas.

**Contexto Adicional**
Screenshots, mockups, exemplos de outros sistemas.
```

---

## Configuração do Ambiente

### Requisitos

-   Java 21
-   Maven 3.9+
-   Docker
-   Git

### Setup

```bash
# Clone
git clone https://github.com/marcusPrado02/cinelog.git
cd cinelog

# Instale dependências
./mvnw clean install

# Inicie infraestrutura
docker-compose up -d

# Execute aplicação
./mvnw spring-boot:run

# Execute testes
./mvnw test
```

---

## Qualidade de Código

### Ferramentas Configuradas

1. **Checkstyle** - Estilo de código
2. **PMD** - Análise estática
3. **SpotBugs** - Bugs potenciais
4. **JaCoCo** - Cobertura de testes

### Executar Verificações

```bash
# Todas as verificações
./mvnw verify

# Apenas Checkstyle
./mvnw checkstyle:check

# Apenas PMD
./mvnw pmd:check

# Apenas SpotBugs
./mvnw spotbugs:check

# Cobertura
./mvnw jacoco:report
```

### Padrões

-   **Cobertura mínima**: 80%
-   **Complexidade ciclomática**: ≤ 10
-   **Tamanho de método**: ≤ 50 linhas
-   **Tamanho de classe**: ≤ 500 linhas

---

## Documentação

### Javadoc

```java
/**
 * Cria uma nova mídia no sistema.
 *
 * @param command dados da mídia a ser criada
 * @return mídia criada com ID gerado
 * @throws ValidationException se os dados forem inválidos
 * @throws DuplicateMediaException se já existir mídia com mesmo título
 */
public Media create(CreateMediaCommand command) {
    // ...
}
```

### Markdown

Toda documentação deve ser em Markdown:

-   README.md
-   docs/\*.md
-   Comentários em código complexo

---

## Comunicação

### Canais

-   **Issues**: Bugs e features
-   **Discussions**: Perguntas e ideias
-   **Pull Requests**: Code review
-   **Email**: contato@cinelog.com

### Etiqueta

-   Seja claro e conciso
-   Forneça contexto suficiente
-   Seja respeitoso
-   Evite off-topic

---

## Reconhecimento

Contribuidores são reconhecidos:

1. **Contributors.md** - Lista de todos os contribuidores
2. **Changelog** - Menção em releases
3. **Social Media** - Agradecimentos públicos

---

## Licença

Ao contribuir, você concorda que suas contribuições serão licenciadas sob a licença MIT do projeto.

---

## Perguntas?

Se tiver dúvidas:

1. Verifique a [documentação](./docs/)
2. Abra uma [Discussion](https://github.com/marcusPrado02/cinelog/discussions)
3. Entre em contato: contato@cinelog.com

---

**Obrigado por contribuir! 🎉**

---

**Última atualização**: Dezembro 2025
