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

COPY --from=builder /app/build/libs/*.jar ./app.jar

# Создаём группу adm с GID 4 (как на хосте, проверить можно через getent group adm | cut -d: -f3)
RUN addgroup -g 4 -S adm 2>/dev/null || true

# Создаём основную группу и пользователя бота
RUN addgroup -g 1001 -S botgroup && \
    adduser -u 1001 -S -G botgroup botuser

# Добавляем пользователя в группу adm (дополнительная группа)
RUN adduser botuser adm

# Переключаемся на непривилегированного пользователя
USER botuser

# Переменные окружения (будут переданы при запуске)
ENV BOT_TOKEN=""
ENV ALLOWED_USER_IDS=""

ENTRYPOINT ["sh", "-c", "java -jar app.jar"]