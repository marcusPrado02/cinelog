#!/bin/bash
# Entrypoint para o container CineLog.
# Corrige incompatibilidades no SPRING_APPLICATION_JSON injetado pelo SCDF 2.11.x:
# - Driver: MariaDB -> MySQL Connector/J
# - Table prefix: TASK_/BATCH_ -> BOOT3_TASK_/BOOT3_BATCH_ (Boot 3)
# - Task init: false -> true
# - Profile: adiciona 'task' se nao presente
# - Redis/Mail: hosts na rede Docker

# Carregar variaveis dos .env files (seletivamente)
for envfile in /app/.env /app/.env.mail; do
    if [ -f "$envfile" ]; then
        [ -z "${TMDB_API_KEY:-}" ] && TMDB_API_KEY=$(grep "^TMDB_API_KEY=" "$envfile" 2>/dev/null | sed 's/^TMDB_API_KEY=//')
        [ -z "${MAIL_HOST:-}" ] && MAIL_HOST=$(grep "^export MAIL_HOST=" "$envfile" 2>/dev/null | sed 's/^export MAIL_HOST=//')
        [ -z "${MAIL_PORT:-}" ] && MAIL_PORT=$(grep "^export MAIL_PORT=" "$envfile" 2>/dev/null | sed 's/^export MAIL_PORT=//')
        [ -z "${MAIL_USER:-}" ] && MAIL_USER=$(grep "^export MAIL_USER=" "$envfile" 2>/dev/null | sed 's/^export MAIL_USER=//')
        [ -z "${MAIL_PASS:-}" ] && MAIL_PASS=$(grep "^export MAIL_PASS=" "$envfile" 2>/dev/null | sed 's/^export MAIL_PASS=//')
    fi
done

if [ -n "${SPRING_APPLICATION_JSON:-}" ]; then
    # Fix driver: MariaDB -> MySQL
    SPRING_APPLICATION_JSON=$(echo "$SPRING_APPLICATION_JSON" | \
        sed 's/"spring.datasource.driverClassName":"org.mariadb.jdbc.Driver"/"spring.datasource.driverClassName":"com.mysql.cj.jdbc.Driver"/g')

    # Fix table prefixes: TASK_/BATCH_ -> BOOT3_TASK_/BOOT3_BATCH_
    SPRING_APPLICATION_JSON=$(echo "$SPRING_APPLICATION_JSON" | \
        sed 's/"spring.cloud.task.tablePrefix":"TASK_"/"spring.cloud.task.tablePrefix":"BOOT3_TASK_"/g' | \
        sed 's/"spring.batch.jdbc.table-prefix":"BATCH_"/"spring.batch.jdbc.table-prefix":"BOOT3_BATCH_"/g')

    # Fix task init: false -> true
    SPRING_APPLICATION_JSON=$(echo "$SPRING_APPLICATION_JSON" | \
        sed 's/"spring.cloud.task.initialize-enabled":"false"/"spring.cloud.task.initialize-enabled":"true"/g')

    # Add profile task, Redis, Mail (Gmail se configurado, senao MailHog), TMDB key
    if [ -n "${MAIL_HOST:-}" ] && [ "$MAIL_HOST" != "mailhog" ]; then
        EXTRA='"spring.profiles.active":"task","spring.data.redis.host":"redis"'
        EXTRA="${EXTRA},\"spring.mail.host\":\"${MAIL_HOST}\""
        EXTRA="${EXTRA},\"spring.mail.port\":\"${MAIL_PORT:-587}\""
        EXTRA="${EXTRA},\"spring.mail.username\":\"${MAIL_USER}\""
        EXTRA="${EXTRA},\"spring.mail.password\":\"${MAIL_PASS}\""
        EXTRA="${EXTRA},\"spring.mail.properties.mail.smtp.auth\":\"true\""
        EXTRA="${EXTRA},\"spring.mail.properties.mail.smtp.starttls.enable\":\"true\""
    else
        EXTRA='"spring.profiles.active":"task","spring.data.redis.host":"redis","spring.mail.host":"mailhog","spring.mail.port":"1025"'
    fi
    if [ -n "${TMDB_API_KEY:-}" ]; then
        EXTRA="${EXTRA},\"cinelog.tmdb.bearer-token\":\"${TMDB_API_KEY}\""
    fi
    SPRING_APPLICATION_JSON=$(echo "$SPRING_APPLICATION_JSON" | \
        sed "s/}$/,${EXTRA}}/g")

    export SPRING_APPLICATION_JSON
fi

# Fix SCDF's --app.cinelog.* args:
# - Drop SCDF deployer args (spring.cloud.*, spring.batch.*)
# - Convert app args (cinelog.*) to Spring Boot format
FIXED_ARGS=()
for arg in "$@"; do
    case "$arg" in
        --app.cinelog.spring.cloud.*|--app.cinelog.spring.batch.*|--app.cinelog.spring.cloud.deployer.*)
            # Drop: already fixed in SPRING_APPLICATION_JSON
            ;;
        --app.cinelog.*)
            # Convert: --app.cinelog.X=Y -> --X=Y (for custom app properties like cinelog.tmdb.*)
            FIXED_ARGS+=("--${arg#--app.cinelog.}")
            ;;
        *schemaTarget=boot2*|*bootVersion=2*)
            ;;
        *)
            FIXED_ARGS+=("$arg")
            ;;
    esac
done

# Inject unique run.id so each SCDF launch creates a new JobInstance.
# Without this, RunIdIncrementer may fail for jobs that were initially created
# without run.id, causing NOOP or JobExecutionAlreadyRunningException.
FIXED_ARGS+=("--run.id=$(date +%s%3N)")

exec java -jar /app/app.jar "${FIXED_ARGS[@]}"
