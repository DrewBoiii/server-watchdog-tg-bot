# Этап 1: Сборка
FROM gradle:8.13-jdk21 AS builder
WORKDIR /app
COPY . .
# Даем права на выполнение gradlew, если он есть, либо используем системный gradle
RUN if [ -f gradlew ]; then chmod +x gradlew; fi
RUN gradle build --no-daemon -x test

# Этап 2: Запуск
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# su-exec — корректный сброс привилегий для Alpine (сигналы и PID 1 работают)
RUN apk add --no-cache su-exec

COPY --from=builder /app/build/libs/app.jar ./app.jar
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Создаём только основную группу и пользователя бота.
# Группы adm и docker будут созданы динамически в entrypoint.sh.
RUN addgroup -g 1001 -S botgroup && \
    adduser -u 1001 -S -G botgroup botuser

# Переменные окружения (будут переданы при запуске)
ENV BOT_TOKEN=""
ENV ALLOWED_USER_IDS=""
# Опциональные «ручные» GID, если автоопределение по файлам не срабатывает
ENV ADM_GID=""
ENV DOCKER_GID=""

# Entrypoint запускается от root, чтобы иметь возможность создавать группы
ENTRYPOINT ["/entrypoint.sh"]