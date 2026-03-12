#!/usr/bin/env bash
# Inicia a aplicação no perfil dev carregando variáveis do .env (gitignored).
# Uso: ./scripts/run-dev.sh [args extras do Maven]
#
# Pré-requisitos:
#   - .env preenchido (copie .env.example se não existir)
#   - docker compose up -d db redis keycloak
#   - docker compose -f docker/docker-compose.dev.yml up -d zookeeper kafka

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="$ROOT_DIR/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERRO: arquivo .env não encontrado em $ROOT_DIR"
  echo "Crie-o a partir do template: cp .env.example .env"
  exit 1
fi

set -a
# shellcheck source=../.env
source "$ENV_FILE"
set +a

echo "Iniciando CinelogApplication com perfil: ${SPRING_PROFILES_ACTIVE:-dev}"

exec "$ROOT_DIR/mvnw" spring-boot:run \
  -Dspring-boot.run.profiles="${SPRING_PROFILES_ACTIVE:-dev}" \
  "$@"
