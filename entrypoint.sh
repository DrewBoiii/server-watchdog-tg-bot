#!/bin/sh
set -e

# 1. Проверяем, что запущены от root (нужно для создания групп и смены пользователя)
if [ "$(id -u)" != "0" ]; then
    echo "ERROR: entrypoint must be run as root" >&2
    exit 1
fi

# 2. Определяем GID для группы adm
#    Пробуем распространённые пути к логу аутентификации.
#    В Ubuntu с systemd-journald файла auth.log может не быть — тогда используем /var/log/secure или /var/log/syslog.
ADM_GID_RESOLVED=""
for p in /var/log/auth.log /var/log/secure /var/log/syslog; do
    if [ -e "$p" ]; then
        ADM_GID_RESOLVED=$(stat -c "%g" "$p")
        echo "INFO: adm GID=$ADM_GID_RESOLVED (determined from $p)"
        break
    fi
done

if [ -z "$ADM_GID_RESOLVED" ]; then
    if [ -n "$ADM_GID" ]; then
        ADM_GID_RESOLVED="$ADM_GID"
        echo "INFO: adm GID=$ADM_GID_RESOLVED (from env ADM_GID)"
    else
        ADM_GID_RESOLVED=4
        echo "WARN: no auth log found and ADM_GID not set; using default GID 4" >&2
    fi
fi

# 3. Определяем GID для группы docker из смонтированного сокета
if [ -S /var/run/docker.sock ]; then
    DOCKER_GID=$(stat -c "%g" /var/run/docker.sock)
    echo "INFO: docker GID=$DOCKER_GID (determined from /var/run/docker.sock)"
elif [ -n "$DOCKER_GID" ]; then
    echo "INFO: docker GID=$DOCKER_GID (from env DOCKER_GID)"
else
    DOCKER_GID=109
    echo "WARN: docker.sock not found and DOCKER_GID not set; using default GID 109" >&2
fi

# 4. Хелпер: гарантировать нужную группу и добавить в неё botuser
#    В Alpine Linux нет usermod/groupmod, поэтому используем addgroup/delgroup.
ensure_group() {
    name="$1"
    gid="$2"

    # Если группа с таким GID уже существует (например, adm:4 в Alpine) — переиспользуем её
    existing_name=$(getent group "$gid" | cut -d: -f1)
    if [ -n "$existing_name" ]; then
        echo "INFO: group with GID $gid already exists as '$existing_name'"
        adduser botuser "$existing_name" 2>/dev/null || true
        return
    fi

    # Если группа с нужным именем есть, но GID другой — пересоздаём
    if getent group "$name" >/dev/null; then
        echo "INFO: group '$name' exists with wrong GID, recreating with GID $gid"
        delgroup "$name" 2>/dev/null || true
    fi

    addgroup -g "$gid" -S "$name"
    adduser botuser "$name" 2>/dev/null || true
}

# 5. Применяем правила для adm и docker
ensure_group adm    "$ADM_GID_RESOLVED"
ensure_group docker "$DOCKER_GID"

echo "INFO: botuser groups: $(id -G botuser 2>/dev/null || true)"

# 6. Сброс привилегий и запуск приложения
exec su-exec botuser java -jar /app/app.jar