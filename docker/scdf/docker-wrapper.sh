#!/bin/bash
# Wrapper para Docker CLI dentro do SCDF/Skipper container.
#
# Funcoes:
# 1. Substitui '--network bridge' por '--network cinelog_default'
#    (resolve limitacao do SCDF Local Deployer 2.11.x que hardcoda rede 'bridge')
# 2. Adiciona '--rm' em 'docker run' para garantir remocao do container ao sair
#    (mais confiavel que DELETE_CONTAINER_ON_EXIT do Skipper)

REAL_DOCKER=/usr/local/bin/docker-real
ARGS=()
SKIP_NEXT=false
IS_RUN=false
HAS_RM=false

# Detecta se e um comando 'docker run' e se ja tem --rm
for arg in "$@"; do
    if [ "$arg" = "run" ] && [ ${#ARGS[@]} -le 1 ]; then
        IS_RUN=true
    fi
    if [ "$arg" = "--rm" ]; then
        HAS_RM=true
    fi
done

for arg in "$@"; do
    if $SKIP_NEXT; then
        # Este arg e o valor de --network, substituir bridge por cinelog_default
        if [ "$arg" = "bridge" ]; then
            ARGS+=("cinelog_default")
        else
            ARGS+=("$arg")
        fi
        SKIP_NEXT=false
        continue
    fi

    case "$arg" in
        --network)
            ARGS+=("$arg")
            SKIP_NEXT=true
            ;;
        run)
            ARGS+=("$arg")
            # Injeta --rm apos 'run' se ainda nao presente
            if $IS_RUN && ! $HAS_RM; then
                ARGS+=("--rm")
            fi
            ;;
        *)
            ARGS+=("$arg")
            ;;
    esac
done

exec "$REAL_DOCKER" "${ARGS[@]}"
