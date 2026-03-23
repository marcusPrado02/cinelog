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
ENV_MAIL_FILE="$ROOT_DIR/.env.mail"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERRO: arquivo .env não encontrado em $ROOT_DIR"
  echo "Crie-o a partir do template: cp .env.example .env"
  exit 1
fi

set -a
# shellcheck source=../.env
source "$ENV_FILE"

# Se existir .env.mail, carrega as configurações de e-mail real
if [[ -f "$ENV_MAIL_FILE" ]]; then
  echo "📧 Carregando configurações de e-mail real (.env.mail)..."
  # shellcheck source=../.env.mail
  source "$ENV_MAIL_FILE"
  echo "   REMETENTE: ${MAIL_USER:-não configurado}"
  echo "   SERVIDOR: ${MAIL_HOST:-localhost}:${MAIL_PORT:-1025}"
  echo "   DESTINATÁRIO (testes): marcus.prado@pitang.com"
  echo ""
else
  echo "📧 Usando MailHog (desenvolvimento) - E-mails em http://localhost:8025"
  echo "   Para enviar e-mails reais, execute: ./scripts/setup-email-real.sh"
  echo ""
fi
set +a

# ── Kill processo na porta 8080 se existir ──────────────────────────────────
PORT=8080
PID=$(lsof -ti :"$PORT" 2>/dev/null || true)
if [[ -n "$PID" ]]; then
  echo "⚠️  Porta $PORT em uso pelo PID $PID — encerrando..."
  kill "$PID" 2>/dev/null || true
  # Aguardar ate 5s para encerrar gracefully
  for i in {1..10}; do
    if ! lsof -ti :"$PORT" > /dev/null 2>&1; then
      echo "   Processo encerrado."
      break
    fi
    sleep 0.5
  done
  # Force kill se ainda estiver rodando
  if lsof -ti :"$PORT" > /dev/null 2>&1; then
    echo "   Forçando encerramento (kill -9)..."
    kill -9 "$(lsof -ti :"$PORT")" 2>/dev/null || true
    sleep 1
  fi
fi

echo "Iniciando CinelogApplication com perfil: ${SPRING_PROFILES_ACTIVE:-dev}"

exec "$ROOT_DIR/mvnw" spring-boot:run \
  -Dspring-boot.run.profiles="${SPRING_PROFILES_ACTIVE:-dev}" \
  "$@"
